package com.kherio.padelmatch.data

/**
 * Genera la siguiente ronda de un torneo según su formato.
 * - AMERICANO: emparejamientos aleatorios, evitando repetir compañero
 *   siempre que sea posible.
 * - MEXICANO: empareja según clasificación actual (1º+4º vs 2º+3º
 *   dentro de cada grupo de 4 jugadores más cercano en puntos).
 *
 * Si el número de jugadores no es múltiplo de 4, los sobrantes
 * descansan esa ronda (rotando quién descansa).
 */
object PairingEngine {

    fun nextRound(tournament: Tournament): TournamentRound {
        val roundNumber = tournament.rounds.size + 1
        val maxPlayersInPlay = tournament.courts * 4

        val playersForRound: List<Player>
        val sittingOut: List<Player>

        if (tournament.players.size <= maxPlayersInPlay) {
            // Todos juegan, pero si no es múltiplo de 4 alguno descansa igualmente
            val playable = (tournament.players.size / 4) * 4
            val ordered = orderForSitOutRotation(tournament)
            playersForRound = ordered.take(playable)
            sittingOut = ordered.drop(playable)
        } else {
            val ordered = orderForSitOutRotation(tournament)
            playersForRound = ordered.take(maxPlayersInPlay)
            sittingOut = ordered.drop(maxPlayersInPlay)
        }

        val groupsOfFour = playersForRound.chunked(4)
        val matches = mutableListOf<MatchResult>()

        groupsOfFour.forEachIndexed { index, group ->
            if (group.size < 4) return@forEachIndexed
            val court = index + 1
            val (team1, team2) = when (tournament.format) {
                TournamentFormat.AMERICANO -> pickAmericanoPairs(group, tournament.pastPairHistory)
                TournamentFormat.MEXICANO -> pickMexicanoPairs(group)
            }
            matches.add(MatchResult(court = court, team1 = team1, team2 = team2))
        }

        return TournamentRound(
            number = roundNumber,
            matches = matches,
            sittingOutPlayerIds = sittingOut.map { it.id }
        )
    }

    /** Prioriza que jueguen quienes menos partidos llevan (rotación justa de descansos). */
    private fun orderForSitOutRotation(tournament: Tournament): List<Player> {
        return tournament.players.sortedBy { it.gamesPlayed }
    }

    /** Para Americano: entre las 3 combinaciones posibles de un grupo de 4,
     * elige la que menos repita parejas ya jugadas. */
    private fun pickAmericanoPairs(group: List<Player>, history: Set<String>): Pair<TeamPairing, TeamPairing> {
        val (a, b, c, d) = listOf(group[0], group[1], group[2], group[3])
        val options = listOf(
            Pair(TeamPairing(a.id, b.id), TeamPairing(c.id, d.id)),
            Pair(TeamPairing(a.id, c.id), TeamPairing(b.id, d.id)),
            Pair(TeamPairing(a.id, d.id), TeamPairing(b.id, c.id))
        )
        return options.minByOrNull { (t1, t2) ->
            val repeats1 = if (pairKey(t1.player1Id, t1.player2Id) in history) 1 else 0
            val repeats2 = if (pairKey(t2.player1Id, t2.player2Id) in history) 1 else 0
            repeats1 + repeats2
        } ?: options.first()
    }

    /** Para Mexicano: dentro de cada grupo de 4 ya ordenado por ranking,
     * 1º+4º contra 2º+3º (formato clásico Mexicano). */
    private fun pickMexicanoPairs(group: List<Player>): Pair<TeamPairing, TeamPairing> {
        val ranked = group.sortedByDescending { it.totalPoints }
        val team1 = TeamPairing(ranked[0].id, ranked[3].id)
        val team2 = TeamPairing(ranked[1].id, ranked[2].id)
        return team1 to team2
    }

    /** Aplica un resultado de partido: suma puntos y partidos jugados a los 4 jugadores. */
    fun applyResult(tournament: Tournament, match: MatchResult): Tournament {
        val score1 = match.score1 ?: return tournament
        val score2 = match.score2 ?: return tournament

        val updatedPlayers = tournament.players.map { player ->
            when (player.id) {
                match.team1.player1Id, match.team1.player2Id ->
                    player.copy(totalPoints = player.totalPoints + score1, gamesPlayed = player.gamesPlayed + 1)
                match.team2.player1Id, match.team2.player2Id ->
                    player.copy(totalPoints = player.totalPoints + score2, gamesPlayed = player.gamesPlayed + 1)
                else -> player
            }
        }

        val newHistory = tournament.pastPairHistory +
            pairKey(match.team1.player1Id, match.team1.player2Id) +
            pairKey(match.team2.player1Id, match.team2.player2Id)

        return tournament.copy(players = updatedPlayers, pastPairHistory = newHistory)
    }
}
