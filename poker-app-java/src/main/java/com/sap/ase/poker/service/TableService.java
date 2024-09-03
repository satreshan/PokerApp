package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.PokerTable;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Service
public class TableService extends PokerTable {

    public TableService(Supplier<Deck> deckSupplier) {
        super(deckSupplier);
    }

    public Map<String, Integer> getBets() {
        return bets;
    }

    public int getPot() {
        return pot;
    }

    public Optional<Player> getWinner() {
        return winner;
    }

    public List<Card> getWinnerHand() {
        if (getWinner().isPresent() && communityCards.size() == 5) {
            return getWinner().get().getHandCards();
        } else {
            return Collections.emptyList();
        }
    }


    public GameState getState() {
        return gameState;
    }

    public List<Player> getPlayers() {
        return playersJoinedTable;
    }

    public List<Card> getPlayerCards(String playerId) {
        Optional<Player> player = playersJoinedTable.stream().filter((p) -> p.getId().equals(playerId)).findFirst();
        if (player.isPresent()) {
            return player.get().getHandCards();
        }
        return Collections.emptyList();
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public List<Card> getCommunityCards() {
        return communityCards;
    }

    public Optional<Player> getCurrentPlayer() {
        return Optional.ofNullable(this.currentPlayer);
    }

    public void addPlayer(String playerId, String playerName) {
        playersJoinedTable.add(new Player(playerId, playerName, initialCashToPlayer));
        bets.put(playerId, 0);
    }

    public void start() {
        if (playersJoinedTable.size() < 2) {
            throw new IllegalActionException("Need at least two players to start the game.");
        }

        for (Player player : playersJoinedTable) {
            player.setHandCards(Arrays.asList(deck.draw(), deck.draw()));
            player.setActive();
            bets.put(player.getId(), 0);
        }

        this.gameState = GameState.PRE_FLOP;
        currentPlayer = playersJoinedTable.get(0);
        currentPlayerIndex = 0;
    }

    public void performAction(String action, int amount) throws IllegalAmountException {
        switch (action) {
            case "check": {
                check();
                break;
            }
            case "call": {
                call();
                break;
            }
            case "fold": {
                fold();
                break;
            }
            case "raise": {
                raise(amount);
                break;
            }
            default:
                throw new IllegalActionException("action not supported.");
        }
    }

}
