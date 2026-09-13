package com.kherio.padelmatch.data

import java.util.UUID

enum class TournamentFormat { AMERICANO, MEXICANO }

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val totalPoints: Int = 0,
    val gamesPlayed: Int = 0
)

// A team is just two player ids paired for one match
data class TeamPairing(
    val player1Id: String,
    val player2Id: String
)

data class MatchResult(
    val id: String = UUID.randomUUID().toString(),
    val court: Int,
    val team1: TeamPairing,
    val team2: TeamPairing,
    var score1: Int? = null,
    var score2: Int? = null
) {
    val isFinished: Boolean get() = score1 != null && score2 != null
}

data class TournamentRound(
    val number: Int,
    val matches: List<MatchResult>,
    val sittingOutPlayerIds: List<String> = emptyList()
)

data class Tournament(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val format: TournamentFormat,
    val courts: Int,
    var players: List<Player>,
    var rounds: List<TournamentRound> = emptyList(),
    var pastPairHistory: Set<String> = emptySet(), // "id1|id2" pairs already used together (Americano)
    var isFinished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun pairKey(a: String, b: String): String {
    val (x, y) = if (a < b) a to b else b to a
    return "$x|$y"
}
