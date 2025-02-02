package indigo

import java.util.Scanner

data class PickCardReturnValue(val card : Card?, val shouldExit : Boolean)

data class Card(val suit : String, val rank : String) {
    override fun toString(): String {
        return "$rank$suit"
    }
}
data class roundResult(val exitGame : Boolean, val playerExited :Boolean)

open class Player(val name : String, val cards : MutableList<Card> =  mutableListOf(), val cardsWon : MutableList<Card> = mutableListOf()) {

    var points : Int = 0

    open fun pickCard(table: List<Card>)  : Card {
        val scanner = Scanner(System.`in`)
        var removeIndex : Int = -1;
        var input : String = ""
        var card: Card  = Card("exit", "exit")
        println("Cards in hand: ${ cards.withIndex().joinToString(separator = " ") { (index, card) -> "${index+1})$card"}}")

        do {
            println("Choose a card to play (1-${cards.size}):")

            input = scanner.nextLine().lowercase()

            if (input == "exit") {
                break
            } else {
                removeIndex = try {
                    input.toInt()
                } catch (e: Exception) {
                    -1
                }
            }


        } while (removeIndex !in 1 ..  cards.size)

        if (removeIndex >= 0) {
            card = cards.removeAt(removeIndex-1)
        }

        return card
    }

}

class Computer(name: String, cards : MutableList<Card> =  mutableListOf()): Player(name, cards) {

    override fun pickCard(table: List<Card>): Card {
        println(cards.joinToString(separator = " "))

        val chosenCard: Card = if (cards.size == 1) {
            cards.first()
        } else if (table.isNotEmpty()) {
            val topCard = table.last()
            val candidateCards = cards.filter { card -> topCard.suit == card.suit || topCard.rank == card.rank }

            if (candidateCards.size == 1) {
                candidateCards.first()
            }else if (candidateCards.size >= 2) {
                tactic2(topCard, candidateCards)
            } else { // no candidate cards exist
                tactic1()
            }

        } else {
            tactic1()
        }
        cards.remove(chosenCard)
        return   chosenCard

    }

    private fun tactic2(topCard : Card, candidateCards : List<Card>) : Card {
        val suitCards = candidateCards.filter { card -> card.suit == topCard.suit }
        val rankCards = candidateCards.filter { card -> card.rank == topCard.rank }

        return  if (suitCards.size >= 2) {
            suitCards.random()
        } else if (rankCards.size >= 2) {
            rankCards.random()
        } else {
            candidateCards.random()
        }
    }

    private fun tactic1() : Card {
        val l_suit = listOf<Char>('♦' ,'♥' ,'♠' ,'♣')
        val l_rank = listOf<String>("A", "2", "3", "4", "5","6", "7", "8", "9", "10", "J" ,"Q", "K")

        val suitCards = l_suit.map { suit -> cards.filter { card -> card.suit ==  suit.toString()} }
        val chosenGroup = suitCards.find { list -> list.size >=2 } ?: listOf()

        return if (chosenGroup.isNotEmpty()) {
            chosenGroup.random()
        } else {
            val rankCards = l_rank.map { rank -> cards.filter { card -> card.rank ==  rank.toString()} }
            val group = suitCards.find { list -> list.size >=2 } ?: listOf()

            if (group.isNotEmpty()) {
                group.random()
            } else {
                cards.random()
            }
        }

        /*  val chosenCard: Card
          if (table.isNotEmpty()) {
              val topCard = table.last()
              val winningCards = cards.filter { card -> topCard.suit == card.suit || topCard.rank == card.rank }
              val priorityCards = winningCards.filter { card -> card.suit == topCard.suit }

              chosenCard = if (priorityCards.isNotEmpty()) {
                  priorityCards.random()
              } else if (winningCards.isNotEmpty()) {
                  winningCards.random()
              } else {
                  cards.random()
              }
          } else {
              chosenCard = cards.random()
          }
          return chosenCard*/
    }







}

enum class RoundState {
    EXIT_GAME,
    PLAYER_EXIT_GAME,
    ONGOING
}

class Game {
    val pointRanks  = listOf("A","10","J","Q","K")
    val l_rank = listOf<String>("A", "2", "3", "4", "5","6", "7", "8", "9", "10", "J" ,"Q", "K")
    val l_suit = listOf<Char>('♦' ,'♥' ,'♠' ,'♣')
    val l_Orgcard = l_rank.flatMap { rank -> l_suit.map { suit -> Card(suit.toString(),rank) } }

    var deck = l_Orgcard.shuffled().toMutableList()
    val computer = Computer("Computer",fetchCards(deck, 6)) //sublist still holds the reference to the orignal list thus we need to make a copy
    val player = Player("Player",fetchCards(deck, 6))
    val table = Player("Table",fetchCards(deck, 4))
    var curPlayer : Player = player;
    var lastWinner : Player? = null;
    var firsTurn : Player = player;


    fun init() {
        println("Indigo Card Game")
        pickFirstPlayer()
        println("Initial cards on the table: ${table.cards.joinToString(" ")}")

        var state = runRound()
        while(state == RoundState.ONGOING){
            state = runRound()
        }

        if (state != RoundState.PLAYER_EXIT_GAME) {
            printScore(gameEnded = true)
        }

        println("Game Over")
    }


    private fun displayTable() {
        if (table.cards.isEmpty()) {
            println("No cards on the table")
        } else if (table.cards.size == 52) {
            println("52 cards on the table, and the top card is ${table.cards.last()}")
        } else {
            println("${table.cards.size} cards on the table, and the top card is ${table.cards.last()}")
        }
    }

    //TODO: Clean up this function...
    private  fun updatePoints(gameEnded: Boolean = false) {
        listOf(computer, player).forEach { ply ->
            ply.points =
                ply.cardsWon.filter { card -> pointRanks.contains(card.rank) }.size //each card with a pointRank is worth 1 point}
        }





        if (gameEnded) {
            val giveRemainingTo =  lastWinner ?: firsTurn
            giveRemainingTo.cardsWon.addAll(table.cards)

            listOf(computer, player).forEach { ply ->
                ply.points =
                    ply.cardsWon.filter { card -> pointRanks.contains(card.rank) }.size //each card with a pointRank is worth 1 point}
            }

            val bonusWinner =  if (player.cardsWon.size == computer.cardsWon.size) {
                lastWinner ?: firsTurn
            } else if (player.cardsWon.size > computer.cardsWon.size) {
                player
            } else {
                computer
            }

            bonusWinner.points += 3
        }




    }

    private fun runRound() : RoundState {
        var state = RoundState.ONGOING

        displayTable()

        val chosenCard : Card = curPlayer.pickCard(table.cards)



        if (curPlayer === computer) {

            println("${computer.name} plays $chosenCard")
        }


        if (chosenCard.suit == "exit") {
            state = RoundState.PLAYER_EXIT_GAME
        } else  { //if (state == RoundState.ONGOING)
            if (table.cards.isEmpty()) {
                table.cards.add(chosenCard)
            } else {
                val topCard =  table.cards.last()
                table.cards.add(chosenCard)
                if ( topCard.suit == chosenCard.suit || topCard.rank == chosenCard.rank) {
                    curPlayer.cardsWon.addAll(table.cards)
                    table.cards.clear()
                    lastWinner = curPlayer
                    println("${curPlayer.name} wins cards")
                    printScore()
                }
            }

            if (deck.isNotEmpty() && player.cards.isEmpty() && computer.cards.isEmpty()) {
                player.cards.addAll(fetchCards(deck, 6))
                computer.cards.addAll(fetchCards(deck, 6))
            } else if (deck.isEmpty() && player.cards.isEmpty() && computer.cards.isEmpty() ) {
                state = RoundState.EXIT_GAME
                displayTable()
            }

            curPlayer = if (curPlayer === computer) player else computer

        }

        return state
    }



    fun printScore(gameEnded  : Boolean = false) {
        updatePoints(gameEnded)
        println("Score: ${player.name} ${player.points} - ${computer.name} ${computer.points}")
        println("Cards: ${player.name} ${player.cardsWon.size} - ${computer.name} ${computer.cardsWon.size}")
    }
    fun pickFirstPlayer() {
        val scanner = Scanner(System.`in`)
        var input : String = ""

        do {
            println("Play first?")
            input = scanner.nextLine().lowercase()
        } while (input!= "yes" && input != "no")

        firsTurn = if (input == "yes") player else computer
        curPlayer = firsTurn
    }
}


fun getCard(deck : List<Card>) : List<Card> {

    var l_newDeck  = deck.toList()
    val scanner = Scanner(System.`in`)
    println("Number of cards:")

    val numToDrop =   try { scanner.nextLine().toInt() }  catch (e: NumberFormatException) { -1};

    if (numToDrop !in 1..52) {
        println("Invalid number of cards.")
    } else if (numToDrop > deck.size) {
        println("The remaining cards are insufficient to meet the request.")
    } else {
        println(l_newDeck.subList(0, numToDrop).joinToString(" "))
        l_newDeck =  l_newDeck.drop(numToDrop)
    }

    return  l_newDeck
}




fun fetchCards(deck :  MutableList<Card>, numOfCards : Int) : MutableList<Card> {
    val cards = deck.subList(0, numOfCards).toMutableList()

    deck.subList(0, numOfCards).clear() //remove cards from deck
    return cards
}




fun main() {
    val game = Game()
    game.init()


}