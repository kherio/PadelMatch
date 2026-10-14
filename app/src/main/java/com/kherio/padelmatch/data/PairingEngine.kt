package com.kherio.padelmatch.data

import kotlin.math.abs

/**
 * Genera la siguiente ronda de un torneo según su formato.
 *
 * - AMERICANO: rotación aleatoria, sin repetir compañero siempre que exista
 *   alguna combinación posible para toda la ronda (se busca a nivel global,
 *   no solo dentro de cada pista).
 * - AMERICANO_COMPETITIVO:
 *     * Ronda 1: rotación aleatoria.
 *     * Desde la ronda 2: se buscan, para TODA la ronda a la vez, las parejas y
 *       los cruces que cumplan, por este orden de prioridad:
 *         1. Ninguna pareja repite compañero (prioridad absoluta).
 *         2. Las dos parejas de cada partido tienen la puntuación conjunta más
 *            cercana posible (se enfrentan parejas de nivel parecido).
 *         3. Los cuatro jugadores de una pista son de nivel parecido entre sí.
 *     * Las pistas se ordenan por nivel: la pista 1 es la de mayor puntuación.
 * - MEXICANO: 1+4 vs 2+3 dentro de cada grupo de cuatro ordenado por ranking.
 *
 * Si hay más jugadores que plazas disponibles, primero se prioriza a quienes
 * menos partidos han jugado. En caso de empate, tienen prioridad los mejor
 * clasificados. Los descansos rotan de esta forma de manera razonablemente justa.
 *
 * El historial de parejas se calcula a partir de TODOS los partidos ya generados
 * en el torneo (con o sin resultado), no solo de los puntuados, para que una
 * pareja no pueda repetirse porque un resultado quedó sin introducir.
 */
object PairingEngine {

    /** Cada pareja repetida cuesta más que cualquier diferencia de puntuación posible. */
    private const val REPEAT_PENALTY = 1_000_000L

    /** Tope de grupos evaluados por la búsqueda, para no bloquear el móvil con torneos grandes. */
    private const val SEARCH_NODE_BUDGET = 150_000

    private data class PlannedMatch(
        val team1: TeamPairing,
        val team2: TeamPairing,
        /** Suma de puntos de los cuatro jugadores; sirve para ordenar las pistas. */
        val strength: Int
    )

    fun nextRound(tournament: Tournament): TournamentRound {
        val roundNumber = tournament.rounds.size + 1
        val maxPlayersInPlay = tournament.courts * 4

        val playersForRound: List<Player>
        val sittingOut: List<Player>

        if (tournament.players.size <= maxPlayersInPlay) {
            val playable = (tournament.players.size / 4) * 4
            val ordered = orderForSitOutRotation(tournament)
            playersForRound = ordered.take(playable)
            sittingOut = ordered.drop(playable)
        } else {
            val ordered = orderForSitOutRotation(tournament)
            playersForRound = ordered.take(maxPlayersInPlay)
            sittingOut = ordered.drop(maxPlayersInPlay)
        }

        val history = playedPairs(tournament)

        val planned: List<PlannedMatch> = when (tournament.format) {
            TournamentFormat.AMERICANO ->
                planMatches(playersForRound.shuffled(), history, competitive = false)

            TournamentFormat.AMERICANO_COMPETITIVO -> {
                val ordered = if (roundNumber == 1) {
                    playersForRound.shuffled()
                } else {
                    byRanking(playersForRound)
                }
                planMatches(ordered, history, competitive = true)
                    // Pista 1 = nivel más alto.
                    .sortedByDescending { it.strength }
            }

            TournamentFormat.MEXICANO ->
                byRanking(playersForRound).chunked(4)
                    .filter { it.size == 4 }
                    .map { group ->
                        val (t1, t2) = pickMexicanoPairs(group)
                        PlannedMatch(t1, t2, group.sumOf { it.totalPoints })
                    }
        }

        val matches = planned.mapIndexed { index, m ->
            MatchResult(court = index + 1, team1 = m.team1, team2 = m.team2)
        }

        return TournamentRound(
            number = roundNumber,
            matches = matches,
            sittingOutPlayerIds = sittingOut.map { it.id }
        )
    }

    /** Prioriza que jueguen quienes menos partidos llevan (rotación justa de descansos). */
    private fun orderForSitOutRotation(tournament: Tournament): List<Player> {
        return tournament.players.sortedWith(
            compareBy<Player> { it.gamesPlayed }
                .thenByDescending { it.totalPoints }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun byRanking(players: List<Player>): List<Player> =
        players.sortedWith(
            compareByDescending<Player> { it.totalPoints }
                .thenBy { it.gamesPlayed }
                .thenBy { it.name.lowercase() }
        )

    /**
     * Todas las parejas (como compañeros) que ya se han formado en el torneo:
     * el historial guardado más los partidos de todas las rondas generadas,
     * puntuados o no.
     */
    private fun playedPairs(tournament: Tournament): Set<String> {
        val fromRounds = tournament.rounds.flatMap { round ->
            round.matches.flatMap { m ->
                listOf(
                    pairKey(m.team1.player1Id, m.team1.player2Id),
                    pairKey(m.team2.player1Id, m.team2.player2Id)
                )
            }
        }
        return tournament.pastPairHistory + fromRounds
    }

    /**
     * Coste de un partido concreto (dos parejas). Cuanto menor, mejor.
     *
     * - Cada pareja ya jugada suma [REPEAT_PENALTY].
     * - En modo competitivo se suma además la diferencia de puntuación entre
     *   las dos parejas (x3) y la dispersión de nivel de los cuatro jugadores.
     */
    private fun matchCost(
        a1: Player, a2: Player,
        b1: Player, b2: Player,
        history: Set<String>,
        competitive: Boolean
    ): Long {
        var cost = 0L
        if (pairKey(a1.id, a2.id) in history) cost += REPEAT_PENALTY
        if (pairKey(b1.id, b2.id) in history) cost += REPEAT_PENALTY
        if (competitive) {
            val s1 = a1.totalPoints + a2.totalPoints
            val s2 = b1.totalPoints + b2.totalPoints
            val hi = maxOf(maxOf(a1.totalPoints, a2.totalPoints), maxOf(b1.totalPoints, b2.totalPoints))
            val lo = minOf(minOf(a1.totalPoints, a2.totalPoints), minOf(b1.totalPoints, b2.totalPoints))
            // La diferencia entre parejas pesa más que la dispersión del grupo.
            cost += abs(s1 - s2).toLong() * 3 + (hi - lo).toLong()
        }
        return cost
    }

    /**
     * Reparte a los jugadores en partidos de 2 contra 2 minimizando el coste
     * total de la ronda. Busca a nivel global (rama y poda), por lo que puede
     * evitar repeticiones que un reparto por pistas fijas no evitaría.
     *
     * [players] debe venir ya ordenado por el criterio deseado (ranking en
     * competitivo, aleatorio en americano): los candidatos se exploran en ese
     * orden, así las primeras soluciones ya son buenas y la poda es eficaz.
     */
    private fun planMatches(
        players: List<Player>,
        history: Set<String>,
        competitive: Boolean
    ): List<PlannedMatch> {
        val n = (players.size / 4) * 4
        if (n == 0) return emptyList()
        val pool = players.take(n)

        var bestCost = Long.MAX_VALUE
        var best: List<PlannedMatch> = emptyList()
        var nodes = 0
        val current = ArrayList<PlannedMatch>()

        fun plan(a: Player, b: Player, c: Player, d: Player, split: Int): PlannedMatch = when (split) {
            // 1+4 contra 2+3 (la más equilibrada si el grupo viene ordenado por ranking)
            0 -> PlannedMatch(TeamPairing(a.id, d.id), TeamPairing(b.id, c.id), 0)
            1 -> PlannedMatch(TeamPairing(a.id, c.id), TeamPairing(b.id, d.id), 0)
            else -> PlannedMatch(TeamPairing(a.id, b.id), TeamPairing(c.id, d.id), 0)
        }.copy(strength = a.totalPoints + b.totalPoints + c.totalPoints + d.totalPoints)

        fun splitCost(a: Player, b: Player, c: Player, d: Player, split: Int): Long = when (split) {
            0 -> matchCost(a, d, b, c, history, competitive)
            1 -> matchCost(a, c, b, d, history, competitive)
            else -> matchCost(a, b, c, d, history, competitive)
        }

        // Solución inicial (reparto por bloques de 4): garantiza que siempre hay una respuesta.
        run {
            var total = 0L
            val greedy = ArrayList<PlannedMatch>()
            pool.chunked(4).forEach { g ->
                var bestSplit = 0
                var bestSplitCost = Long.MAX_VALUE
                for (s in 0..2) {
                    val c = splitCost(g[0], g[1], g[2], g[3], s)
                    if (c < bestSplitCost) { bestSplitCost = c; bestSplit = s }
                }
                total += bestSplitCost
                greedy.add(plan(g[0], g[1], g[2], g[3], bestSplit))
            }
            bestCost = total
            best = greedy
        }

        fun search(remaining: List<Int>, costSoFar: Long) {
            if (bestCost == 0L || nodes >= SEARCH_NODE_BUDGET) return
            if (remaining.isEmpty()) {
                if (costSoFar < bestCost) {
                    bestCost = costSoFar
                    best = ArrayList(current)
                }
                return
            }
            val anchor = remaining[0]
            val rest = remaining.subList(1, remaining.size)
            for (i in 0 until rest.size - 2) {
                for (j in i + 1 until rest.size - 1) {
                    for (k in j + 1 until rest.size) {
                        nodes++
                        val a = pool[anchor]
                        val b = pool[rest[i]]
                        val c = pool[rest[j]]
                        val d = pool[rest[k]]
                        var after: List<Int>? = null
                        for (s in 0..2) {
                            val cost = costSoFar + splitCost(a, b, c, d, s)
                            if (cost >= bestCost) continue
                            if (after == null) {
                                after = rest.filterIndexed { idx, _ -> idx != i && idx != j && idx != k }
                            }
                            current.add(plan(a, b, c, d, s))
                            search(after, cost)
                            current.removeAt(current.lastIndex)
                            if (bestCost == 0L || nodes >= SEARCH_NODE_BUDGET) return
                        }
                    }
                }
            }
        }

        search(pool.indices.toList(), 0L)
        return best
    }

    /** Para Mexicano: dentro de cada grupo de 4 ya ordenado por ranking,
     * 1º+4º contra 2º+3º (formato clásico Mexicano). */
    private fun pickMexicanoPairs(group: List<Player>): Pair<TeamPairing, TeamPairing> {
        val ranked = byRanking(group)
        val team1 = TeamPairing(ranked[0].id, ranked[3].id)
        val team2 = TeamPairing(ranked[1].id, ranked[2].id)
        return team1 to team2
    }

    /**
     * Solo para AMERICANO / AMERICANO_COMPETITIVO: indica si, una vez jugada
     * la ronda indicada, ya se habrán agotado todas las combinaciones de
     * pareja posibles entre los jugadores del torneo (cada jugador ha sido
     * compañero de todos los demás al menos una vez). Con N jugadores hay
     * N*(N-1)/2 parejas distintas posibles; en un torneo de 4 jugadores y
     * 1 pista son exactamente 3 rondas (AB/CD, AC/BD, AD/BC), no más.
     *
     * Es una condición necesaria y, en la práctica (grupos de 4 sin huecos),
     * también suficiente para que todos hayan sido además RIVALES de todos:
     * en un grupo de 4, quien no es tu pareja en una ronda es forzosamente
     * tu rival esa misma ronda.
     */
    fun isRoundRobinComplete(tournament: Tournament, round: TournamentRound): Boolean {
        if (tournament.format == TournamentFormat.MEXICANO) return false
        val n = tournament.players.size
        if (n < 4) return false
        val totalPossiblePairs = n * (n - 1) / 2
        val projectedHistory = playedPairs(tournament) + round.matches.flatMap {
            listOf(
                pairKey(it.team1.player1Id, it.team1.player2Id),
                pairKey(it.team2.player1Id, it.team2.player2Id)
            )
        }
        return projectedHistory.size >= totalPossiblePairs
    }

    /** Aplica un resultado de partido: suma puntos y partidos jugados a los 4 jugadores. */
    fun applyResult(tournament: Tournament, match: MatchResult): Tournament {
        val score1 = match.score1 ?: return tournament
        val score2 = match.score2 ?: return tournament

        val updatedPlayers = tournament.players.map { player ->
            when (player.id) {
                match.team1.player1Id, match.team1.player2Id ->
                    player.copy(
                        totalPoints = player.totalPoints + score1,
                        gamesPlayed = player.gamesPlayed + 1
                    )
                match.team2.player1Id, match.team2.player2Id ->
                    player.copy(
                        totalPoints = player.totalPoints + score2,
                        gamesPlayed = player.gamesPlayed + 1
                    )
                else -> player
            }
        }

        val newHistory = tournament.pastPairHistory +
            pairKey(match.team1.player1Id, match.team1.player2Id) +
            pairKey(match.team2.player1Id, match.team2.player2Id)

        return tournament.copy(players = updatedPlayers, pastPairHistory = newHistory)
    }
}
