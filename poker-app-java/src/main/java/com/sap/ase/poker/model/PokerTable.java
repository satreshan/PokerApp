package com.sap.ase.poker.model;

import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.rules.HandRules;
import com.sap.ase.poker.model.rules.WinnerRules;
import com.sap.ase.poker.model.rules.Winners;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PokerTable implements Playable {
    protected final Supplier<Deck> deckSupplier;
    protected final Deck deck;
    protected final int initialCashToPlayer = 100;
    protected GameState gameState;
    protected List<Player> playersJoinedTable;
    protected Player currentPlayer;
    protected int currentPlayerIndex;
    protected List<Card> communityCards;
    protected Optional<Player> winner;
    protected int pot;

    protected Map<String, Integer> bets;

    protected int currentBet;

    protected boolean gameContinue;

    protected boolean canCall;

    public PokerTable(Supplier<Deck> deckSupplier) {
        this.deckSupplier = deckSupplier;
        this.deck = deckSupplier.get();
        this.gameState = GameState.OPEN;
        this.playersJoinedTable = new LinkedList<>();
        this.currentPlayer = null;
        this.communityCards = new ArrayList<>();
        this.currentBet = 0;
        this.gameContinue = false;
        this.bets = new HashMap<>();
        this.pot = 0;
        this.winner = Optional.ofNullable(null);
    }

    private void initializeBets() {
        playersJoinedTable.stream().forEach(player -> bets.put(player.getId(), 0));
    }


    protected void updateGameState() {
        switch (gameState) {
            case OPEN: {
                initializeBets();
                gameState = GameState.PRE_FLOP;
                break;
            }
            case PRE_FLOP: {
                pot += addBets();
                currentBet = 0;
                communityCards.add(deck.draw());
                communityCards.add(deck.draw());
                communityCards.add(deck.draw());
                gameState = GameState.FLOP;
                break;
            }
            case FLOP: {
                pot += addBets();
                currentBet = 0;
                initializeBets();
                gameState = GameState.TURN;
                communityCards.add(deck.draw());
                break;
            }
            case TURN: {
                pot += addBets();
                currentBet = 0;
                initializeBets();
                gameState = GameState.RIVER;
                communityCards.add(deck.draw());
                break;
            }
            case RIVER: {
                pot += addBets();
                winner = calculateWinner();
                currentBet = 0;
                initializeBets();
                gameState = GameState.ENDED;
                break;
            }
            case ENDED:
                communityCards.clear();
                break;
            default:
                new IllegalStateException("State is illegal");
        }
    }

    private Optional<Player> calculateWinner() {
        HandRules handRules = new HandRules();
        WinnerRules winnerRules = new WinnerRules(handRules);
        Winners winnersList = winnerRules.findWinners(communityCards, getActivePlayers(playersJoinedTable));
        int winningContribution = pot / winnersList.getWinners().size();
        for (Player winner : winnersList.getWinners()) {
            winner.setCash(winner.getCash() + winningContribution);
        }
        return Optional.of(winnersList.getWinners().get(0));
    }

    private List<Player> getActivePlayers(List<Player> playersJoinedTable) {
        return playersJoinedTable.stream().filter(player -> player.isActive()).collect(Collectors.toList());
    }

    private int addBets() {
        int resultBet = 0;
        for (int bet : bets.values()) {
            resultBet += bet;
        }
        return resultBet;
    }

    protected int getNextPlayerIndex(int currentPlayerIndex) {
        int index = (currentPlayerIndex + 1) % playersJoinedTable.size();
        while (!playersJoinedTable.get(index).isActive()) {
            index = getNextPlayerIndex(index);
        }
        return index;
    }

    private boolean isGameContinue(String action) {
        if (!gameContinue && currentPlayerIndex == playersJoinedTable.size() - 1 && action.equals("raise")) {
            gameContinue = true;
            return gameContinue;
        }
        if (!gameContinue && currentPlayerIndex == playersJoinedTable.size() - 1 && !action.equals("raise")) {
            gameContinue = false;
            return gameContinue;
        }
        if (gameContinue && !action.equals("raise")) {
            rotateList();
            gameContinue = false;
            return gameContinue;
        } else {
            return true;
        }
    }

    private void rotateList() {
        Collections.rotate(playersJoinedTable, currentPlayerIndex + 1);
    }

    private void updateCurrentPlayerAndGameState(String action) {
        Player nextPlayer = playersJoinedTable.get(getNextPlayerIndex(currentPlayerIndex));

        //1. when last player raise --- continue---> anything other than raise ---> end round
        //2. last player action is not raise --- end round
        if (!isGameContinue(action)) //&& currentPlayerIndex == getPlayers().size()-1)
        {
            updateGameState();
        }
        currentPlayer = nextPlayer;
        currentPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
    }

    @Override
    public void check() {
        //Check action: Nothing to do
        if (currentBet != 0) {
            throw new IllegalActionException("Can not check if bet is already places");
        }
        updateCurrentPlayerAndGameState("check");
    }

    @Override
    public void call() {
        if (!canCall) {
            throw new IllegalActionException("One of the previous player should call raise action to perform call");
        }
        int betDiff = currentBet - bets.get(currentPlayer.getId());

        currentPlayer.setCash(currentPlayer.getCash() - betDiff);
        bets.put(currentPlayer.getId(), bets.get(currentPlayer.getId()) + betDiff);
        updateCurrentPlayerAndGameState("call");
    }

    @Override
    public void raise(int amount) {
        if (amount < currentBet) {
            throw new IllegalAmountException("Betted amount should be greater than current bet");
        }
        if (amount > currentPlayer.getCash()) {
            throw new IllegalAmountException("Betted amount should be lesser than amount held by player");
        }
        Optional<Player> minBetAllowed = playersJoinedTable.stream()
                                                     .min((player1, player2) -> Integer.compare(player1.getCash(),
                                                             player2.getCash()));
        if (minBetAllowed.get().getCash() < amount) {
            throw new IllegalAmountException("Betted amount should not be more than the least amount held by a player");
        }
        bets.put(currentPlayer.getId(), amount);
        currentPlayer.setCash(currentPlayer.getCash() - amount);
        currentBet = amount;
        canCall = true;

        updateCurrentPlayerAndGameState("raise");
    }

    @Override
    public void fold() {
        currentPlayer.setInactive();
        currentPlayer = playersJoinedTable.get(getNextPlayerIndex(currentPlayerIndex));
        //check if only one active player remaining and end the game
        List<Player> activePlayers = playersJoinedTable.stream()
                                                       .filter(player -> player.isActive())
                                                       .collect(Collectors.toList());
        if (activePlayers.size() == 1) {
            winner = Optional.of(activePlayers.get(0));
            gameState = GameState.ENDED;
        } else {
            updateCurrentPlayerAndGameState("fold");
        }
    }

}
