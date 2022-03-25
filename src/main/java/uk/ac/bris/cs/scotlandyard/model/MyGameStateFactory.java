package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.units.qual.A;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
//import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */

// CLASS
public final class MyGameStateFactory implements Factory<GameState> {
    // BUILD METHOD
    @Nonnull
    @Override
    public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        // build return a new instance of GameState
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
    }

    // FACTORY TO CREATE THE SCOTLANDYARD GAME
    private final class MyGameState implements GameState {
        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private List<Player> detectives;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;
        private ImmutableList<Player> everyone;

        // constructor for my GameState
        // constructor will be called by the build method of the outer class MyGameStateFactory
        // why private?
        private MyGameState(
                final GameSetup setup,
                final ImmutableSet<Piece> remaining, // all the pieces that haven't moved yet
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final List<Player> detectives) {


            // CHECKS
            if (mrX.piece().webColour() != "#000") throw new IllegalArgumentException("MrX not a black piece");
            if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
            if (!mrX.isMrX()) throw new IllegalArgumentException("there is no mrX!");
            if (detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
            if (setup.graph.edges().isEmpty()) throw new IllegalArgumentException("Graph is empty!");

            // check properties of each detective
            for (Player d : detectives) {
                if (!d.isDetective()) throw new IllegalArgumentException("No detective!");
                if (d.has(ScotlandYard.Ticket.DOUBLE))
                    throw new IllegalArgumentException("detectives cant have double");
                if (d.has(ScotlandYard.Ticket.SECRET))
                    throw new IllegalArgumentException("detectives shouldn't have secret ticket");
            }
            // Check duplicated properties of detectives
            // O(n^2) notation can be improved using a hash table to get O(n)
            for (int i = 0; i < detectives.size(); i++) {
                for (int j = i + 1; j < detectives.size(); j++) {
                    // if the next detective doest exist so break
                    if (j > detectives.size()) break;
                    if (detectives.get(i).location() == detectives.get(j).location()) {
                        throw new IllegalArgumentException("Same location!");
                    }
                    if (detectives.get(i).piece() == detectives.get(j).piece()) {
                        throw new IllegalArgumentException("Duplicated game pieces!");
                    }
                }
            }


            // create a list with all the players (detectives + mrx)
            List<Player> everyone = new ArrayList<>();
            everyone.add(mrX);
            everyone.addAll(detectives);

            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.everyone = ImmutableList.copyOf(everyone);
            this.moves = getAvailableMoves();

        }


        //-------------------- Getters --------------------//

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            List<Piece> allPieces = new ArrayList<>();
            allPieces.add(mrX.piece());
            for (Player i : detectives) {
                allPieces.add(i.piece());
            }
            return ImmutableSet.copyOf(allPieces);
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            // For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
            // otherwise, return Optional.empty();
            for (Player p : detectives) {
                if (p.piece().equals(detective)) return Optional.of(p.location());
            }
            return Optional.empty();

        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            ImmutableSet<Piece> players = getPlayers();
            // check if there is the piece
            if (!players.contains(piece)) return Optional.empty();

            if (piece.isMrX()) {
                // check if it has tickets has()
                return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
            }
            if (piece.isDetective()) {
                for (Player player : detectives) {
                    if (player.equals(piece)) {
                        return Optional.of(ticket -> player.tickets().getOrDefault(ticket, 0));
                    }
                }
            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return null;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            //if (!getWinner().isEmpty()) { return ImmutableSet.of(); }
            HashSet<SingleMove> singleMoves = new HashSet<>();
            HashSet<DoubleMove> doubleMoves = new HashSet<>();
            HashSet<Move> moves = new HashSet<>();

            // do we have to iterate through detectives or all the player including MRX?
            for (Player player : everyone) {
                if (remaining.contains(player.piece())) {
                    singleMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
                    doubleMoves.addAll(makeDoubleMoves(setup, detectives, player, player.location(), log));
                }
            }

            moves.addAll(singleMoves);
            moves.addAll(doubleMoves);
            return ImmutableSet.copyOf(moves);
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

            // anon -> we can access to the list of all pieces (data from the constructur)
            //  why and what they are? => for the presentation
            List<LogEntry> newLog = new ArrayList<>(log);
            List<Player> newDetectives = new ArrayList<>();
            List<Piece> oldRemaining = new ArrayList<>(remaining);
            int rounds = 1;

            //int  = rounds;
            int finalRounds = rounds;

            Visitor<Player> v = new Visitor<>() {

                @Override
                public Player visit(SingleMove move) {

                    // to know if the player is mrX or a detective
                    Player player = playerFromPiece(move.commencedBy());

                    // use => lose ticket
                    // at => go to the dest
                    // taking the tickets and move the player to the destination
                    // we are creating a new player with the location and the tickets already taken
                    Player newPlayer = player.use(move.ticket).at(move.destination);

                    if (!player.isMrX()) { // if is a detective
                        // detectivas has to give the tickets to mrx -> RULES
                        // updating mrX with new tickets
                        mrX = mrX.give(move.ticket); // give the ticket to mrx from detectives
                    } else { // if is MRX
                        // check if in this round mrx has to reveal his moves
                        // New log entry
                        //setup.moves list of each round

                        // if moves[rounds] == true reveal mrx moves
                        if(setup.moves.get(finalRounds - 1)) {
                            newLog.add(LogEntry.reveal(move.ticket, move.destination));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket));
                        }

                        // setup.moves => return a list with booleans -> if the boolean is true Mrx has to reveal his moves
                    }
                    return newPlayer;
                }

                @Override
                public Player visit(DoubleMove move) {
                    Player player = playerFromPiece(move.commencedBy());

                    // take the tickets and move to the new location
                    Player newPlayer = player.use(move.ticket1).at(move.destination1);
                    newPlayer = newPlayer.use(move.ticket2).at(move.destination2);

                    // update the log
                    // check rounda and reveal moves

                    if(!player.isMrX()) {
                        // is a detective
                        mrX = mrX.give(move.ticket1); // give the ticket to mrx from detectives
                        mrX = mrX.give(move.ticket2); // give the ticket to mrx from detectives

                        // WHY?
                        newDetectives.set(newDetectives.indexOf(player), newPlayer);
                        oldRemaining.remove(player);
                        if (oldRemaining.isEmpty()) oldRemaining.add(mrX.piece());


                    } else {

                        // if moves[rounds] == true reveal mrx moves
                        if(setup.moves.get(finalRounds - 1)) {
                            newLog.add(LogEntry.reveal(move.ticket1, move.destination1));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket1));
                        }

                    /*
                    Checks once again if we are in a reveal round because we need
                    to add two separate entries, one for each move within the double move
                    */
                        if(setup.moves.get(finalRounds - 1)) {
                            newLog.add(LogEntry.reveal(move.ticket2, move.destination2));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket2));
                        }

                        // WHY?
                        oldRemaining.remove(player);
                        if (oldRemaining.isEmpty()) {
                            for (Player p : detectives) {
                                oldRemaining.add(p.piece());
                            }
                        }

                    }

                    return newPlayer;
                }
            };

            // mrx exam
            Player newPlayer = move.accept(v);
            Player newMrx;
            List<Piece> newRemaining = new ArrayList<>();


            // only update newDetectives if newPlayer is a detective if a mrx do not create
            // by default this loop will always be updated if is a detective
            // change detectives list including the newPlayer
            for(Player p : detectives) {
                if(p.piece() == newPlayer.piece()) {
                    newDetectives.add(newPlayer);
                } else {
                    newDetectives.add(p);
                }
            }

            // exclude the piece moved for remaining
            for(Piece p : remaining) {
                if(p != newPlayer.piece()) {
                    newRemaining.add(p);
                }
            }


            if(newPlayer.isMrX()) {
                newMrx = newPlayer;
            } else {
                newMrx = mrX;
            }


            // beginning of each round, only mrX is in remaining set
            if(move.commencedBy().isMrX()){
                for(Player p : detectives){
                    newRemaining.add(p.piece());
                }
            }
            else{
                for(Piece p : remaining){
                    if (p != move.commencedBy()) newRemaining.add(p);
                }
            }

            // WHY?
            for (Piece piece : oldRemaining) {
                Player player = playerFromPiece(piece);
                if (player == null) continue;
				/* Checks if the piece(s) that can still move in the current round have indeed
                available moves; if they do, then they can play in this round */
                if (!makeSingleMoves(setup, newDetectives, player, player.location()).isEmpty()) {
                    newRemaining.add(piece);
                }
            }
            // WHY?
            if (newRemaining.isEmpty()) newRemaining.add(newMrx.piece());

            if(remaining.isEmpty()) rounds++;


            return new MyGameState(
                    setup,
                    ImmutableSet.copyOf(newRemaining),
                    ImmutableList.copyOf(newLog),
                    newMrx,
                    newDetectives
            );
        }

        // just a help function to  get corresponding player from its piece.
        private Player playerFromPiece(Piece piece) {
            for (Player player : everyone) {
                if (player.piece().equals(piece)) return player;
            }
            return null;
        }

    }


    //-------------------- Auxiliary Functions --------------------//


    // MAKE SINGLE MOVES METHOD
    private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
        HashSet<SingleMove> singleMoves = new HashSet<>();

        for (int destination : setup.graph.adjacentNodes(source)) {
            boolean occupied = false;
            for (Player p : detectives) {
                if (p.location() == destination) occupied = true;
            }
            if (occupied) continue;

            for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                if (player.has(t.requiredTicket())) {
                    SingleMove m = new SingleMove(player.piece(), source, t.requiredTicket(), destination);
                    singleMoves.add(m);
                }
            }
            if (player.has(Ticket.SECRET)) {
                SingleMove m = new SingleMove(player.piece(), source, Ticket.SECRET, destination);
                singleMoves.add(m);
            }
        }
        return singleMoves;
    }

    // MAKE DOUBLE MOVES METHOD
    private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {

        HashSet<DoubleMove> doubleMoves = new HashSet<>();
        Set<Integer> locations = new HashSet<>();

        // store only locations
        for (Player d : detectives) locations.add(d.location());

        if ((player.has(Ticket.DOUBLE)) && (setup.moves.size() - log.size() >= 2)) {
            //piece, source, ticket1, destination1, ticket2, destination2
            for (int destination1 : setup.graph.adjacentNodes(source)) {
                if (locations.contains(destination1)) continue;
                for (Transport t1 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()))) {
                    if (player.has(t1.requiredTicket())) {
                        for (int destination2 : setup.graph.adjacentNodes(destination1)) {
                            if (locations.contains(destination2)) continue;
                            for (Transport t2 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()))) {
                                if (t2.requiredTicket() == t1.requiredTicket()) {
                                    if (player.hasAtLeast(t2.requiredTicket(), 2)) {
                                        DoubleMove doubleMove = new DoubleMove(
                                                player.piece(),
                                                source,
                                                t2.requiredTicket(),
                                                destination1,
                                                t2.requiredTicket(),
                                                destination2);
                                        doubleMoves.add(doubleMove);
                                    }
                                } else if (player.has(t2.requiredTicket())) {
                                    DoubleMove doubleMove = new DoubleMove(
                                            player.piece(),
                                            source,
                                            t1.requiredTicket(),
                                            destination1,
                                            t2.requiredTicket(),
                                            destination2);
                                    doubleMoves.add(doubleMove);
                                }
                            }
                            if (player.has(Ticket.SECRET)) {
                                DoubleMove doubleMove = new DoubleMove(
                                        player.piece(),
                                        source,
                                        t1.requiredTicket(),
                                        destination1,
                                        Ticket.SECRET,
                                        destination2);
                                doubleMoves.add(doubleMove);
                            }
                        }
                    }
                }
            }
        }

        return doubleMoves;
    }

    //-------------------- Auxiliary Classes --------------------//

    public class Visssstor implements Visitor<Void> {

        @Override
        public Void visit(SingleMove move) {
            return null;
        }

        @Override
        public Void visit(DoubleMove move) {
            return null;
        }
    }

}

/*

	private static class VisitorClass implements Visitor<Void> {

		private int destination1;
		private int destination2;

		@Override
		public Void visit(SingleMove move) {

		}

		@Override
		public Void visit(DoubleMove move) {

		}
	}




* */



















/*
			if (setup == null) throw new IllegalArgumentException("Setup is null!");
			if (remaining == null) throw new IllegalArgumentException("Remaining is null!");
			if (log == null) throw new IllegalArgumentException("Log is null!");
			if (mrX == null) throw new IllegalArgumentException("mrX is null!");
			if (detectives == null) throw new IllegalArgumentException("Detectives is null!");

*/


