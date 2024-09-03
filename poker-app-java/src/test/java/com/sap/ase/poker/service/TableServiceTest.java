package com.sap.ase.poker.service;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.CardShuffler;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.deck.PokerCardsSupplier;
import com.sap.ase.poker.model.deck.RandomCardShuffler;
import com.sap.ase.poker.model.deck.ShuffledDeckSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TableServiceTest {
    TableService tableService;

    @BeforeEach
    public void setUp(){
        Supplier<List<Card>> cardSupplier = new PokerCardsSupplier();
        CardShuffler cardShuffler = new RandomCardShuffler();
        Supplier<Deck> deckSupplier = new ShuffledDeckSupplier(cardSupplier, cardShuffler);
        tableService = new TableService(deckSupplier);
    }

    private void addPlayers(){
        tableService.addPlayer("al-capone", "Al Capone");
        tableService.addPlayer("poker-alice", "Poker Alice");
    }

    @Order(1)
    @Test
    public void testGetState(){
        Assertions.assertEquals(GameState.OPEN, tableService.getState());
    }

    @Order(2)
    @Test
    public void testGetPlayers(){
        Assertions.assertEquals(0, tableService.getPlayers().size());
    }

    @Order(3)
    @Test
    public void testAddPlayer(){
        tableService.addPlayer("al-capone", "Al Capone");
        List<Player> playersJoinedTheTable = tableService.getPlayers();
        Assertions.assertEquals(1, tableService.getPlayers().size());
        Assertions.assertEquals("al-capone", playersJoinedTheTable.get(0).getId());
        Assertions.assertEquals("Al Capone", playersJoinedTheTable.get(0).getName());
    }

    @Order(4)
    @Test
    public void testStart(){
        Assertions.assertThrows(IllegalActionException.class, () -> tableService.start());

        //Add 2 players, start game and game state change to PRE_FLOP
        addPlayers();

        tableService.start();
        Assertions.assertEquals(GameState.PRE_FLOP, tableService.getState());

        //Player gets two cards from deck and its state changed to active. First player in the list become current
        // player
        Player player1 = tableService.getPlayers().get(0);
        Player player2 = tableService.getPlayers().get(1);
        Assertions.assertEquals(player1.isActive(), true);
        Assertions.assertEquals(player2.isActive(), true);
        Assertions.assertEquals(2, player1.getHandCards().size());
        Assertions.assertEquals(2, player2.getHandCards().size());
        Assertions.assertTrue(tableService.getBets().size() == 2);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 0);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 0);

        //Check the currentPlayer
        Assertions.assertEquals(tableService.getCurrentPlayer().get().getId(), player1.getId());
    }

    @Test
    @Order(5)
    public void testGetCurrentPlayer(){
        addPlayers();
        tableService.start();
        Player currentPlayer = tableService.getPlayers().get(0);
        Assertions.assertEquals(tableService.getCurrentPlayer().get().getId(), currentPlayer.getId());
    }

    @Test
    @Order(6)
    public void testGetPlayerCards(){
        addPlayers();
        tableService.start();
        List<Card> player1Cards = tableService.getPlayers().get(0).getHandCards();
        List<Card> playerAlCaponeCards = tableService.getPlayerCards("al-capone");
        Assertions.assertTrue(tableService.getPlayerCards("alice-fhjd").isEmpty());
        Assertions.assertTrue(player1Cards.get(0).equals(playerAlCaponeCards.get(0)));
        Assertions.assertTrue(player1Cards.get(1).equals(playerAlCaponeCards.get(1)));
    }

    @Test
    @Order(7)
    public void testGetCommunityCards(){
        addPlayers();
        tableService.start();
        Assertions.assertTrue(tableService.getCommunityCards().isEmpty());
    }

    @Test
    @Order(8)
    public void testPerformActionCheck(){
        addPlayers();
        tableService.start();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        Assertions.assertThrows(IllegalActionException.class, () -> tableService.performAction("bet", 0));
        tableService.performAction("check", 0);
        Assertions.assertTrue(!tableService.getCurrentPlayer().get().getId().equals(currentPlayer.getId()));
        Assertions.assertTrue(tableService.getState().equals(GameState.PRE_FLOP));
        currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("check", 0);
        Assertions.assertTrue(!tableService.getCurrentPlayer().get().getId().equals(currentPlayer.getId()));
        Assertions.assertTrue(tableService.getState().equals(GameState.FLOP));
        Assertions.assertTrue(tableService.getCommunityCards().size() == 3);
        Assertions.assertTrue(tableService.getBets().size() == 2);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 0);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 0);
    }

    @Test
    @Order(9)
    public void testPerformActionFold(){
        addPlayers();
        tableService.start();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("fold", 0);
        Assertions.assertTrue(!tableService.getCurrentPlayer().get().getId().equals(currentPlayer.getId()));
        Assertions.assertTrue(tableService.getState().equals(GameState.ENDED));
        currentPlayer = tableService.getCurrentPlayer().get();
        Assertions.assertTrue(tableService.getWinner().get().getId().equals(currentPlayer.getId()));
        Assertions.assertTrue(tableService.getBets().size() == 2);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 0);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 0);
    }

    @Test
    @Order(10)
    public void testPerformActionRaise(){
        addPlayers();
        tableService.start();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        int betValue = tableService.getCurrentBet();
        tableService.performAction("raise", 10);
        Assertions.assertTrue(betValue < 10);
        Assertions.assertTrue(currentPlayer.getCash() == 90);

        currentPlayer = tableService.getCurrentPlayer().get();
        betValue = tableService.getCurrentBet();
        tableService.performAction("raise", 20);
        Assertions.assertTrue(betValue < 20);
        Assertions.assertTrue(currentPlayer.getCash() == 80);
        Assertions.assertTrue(!tableService.getState().equals(GameState.ENDED));

        //tableService.performAction("raise", 200);
        Assertions.assertThrows(IllegalAmountException.class,()->tableService.performAction("raise", 200));
        Assertions.assertThrows(IllegalAmountException.class,()->tableService.performAction("raise", 10));

        Assertions.assertThrows(IllegalAmountException.class,()->tableService.performAction("raise", 85));
        Assertions.assertThrows(IllegalActionException.class, () -> tableService.performAction("check", 0));
        Assertions.assertTrue(tableService.getBets().size() == 2);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 10);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 20);
    }

    @Test
    @Order(11)
    public void testPerformActionCall(){
        addPlayers();
        tableService.start();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        int betValue = tableService.getCurrentBet();
        Assertions.assertThrows(IllegalActionException.class, () -> tableService.performAction("call", 10));
        tableService.performAction("raise", 20);
        Assertions.assertTrue(betValue < 20);
        Assertions.assertTrue(currentPlayer.getCash() == 80);
        currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("call", tableService.getCurrentBet());
        Assertions.assertTrue(currentPlayer.getCash() == 80);
        Assertions.assertTrue(tableService.getState().equals(GameState.FLOP));
        Assertions.assertTrue(tableService.getBets().size() == 2);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 20);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 20);
    }

    @Test
    @Order(12)
    public void testPerformActionCall1(){
        addPlayers();
        tableService.getPlayers().add(new Player("joe-Biden", "Joe Biden", 100));
        tableService.start();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        tableService.performAction("raise", 20);
        Assertions.assertTrue(currentPlayer.getCash() == 80);

        tableService.performAction("call", tableService.getCurrentBet());
        Assertions.assertTrue(tableService.getCurrentBet() == 20);

        tableService.performAction("raise", 30);
        int betValue = tableService.getCurrentBet();

        Assertions.assertTrue(tableService.getCurrentBet() == betValue);
        Assertions.assertTrue(tableService.getState().equals(GameState.PRE_FLOP));
        Assertions.assertTrue(tableService.getWinnerHand().isEmpty());
        currentPlayer = tableService.getCurrentPlayer().get();

        tableService.performAction("call", 10);
        Assertions.assertTrue(tableService.getBets().size() == 3);
        Assertions.assertTrue(tableService.getBets().get("al-capone") == 30);
        Assertions.assertTrue(tableService.getBets().get("poker-alice") == 20);
        Assertions.assertTrue(tableService.getBets().get("joe-Biden") == 30);
        Assertions.assertTrue(currentPlayer.getCash() == 70);
        Assertions.assertTrue(tableService.getState().equals(GameState.FLOP));



        Map<String, Integer> playersCash = new HashMap<>();
        for (Player p:
             tableService.getPlayers()) {
            playersCash.put(p.getId(), p.getCash());
        }

        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        Assertions.assertTrue(tableService.getState().equals(GameState.TURN));
        //TURN state
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        Assertions.assertTrue(tableService.getState().equals(GameState.RIVER));

        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);


        Assertions.assertTrue(tableService.getState().equals(GameState.ENDED));
        Player winner = tableService.getWinner().get();
        List<Card> winnerHand = tableService.getWinnerHand();
        Assertions.assertTrue(!winnerHand.isEmpty());
        Assertions.assertTrue(winner.getCash() == tableService.getPot() + playersCash.get(winner.getId()));
    }

}