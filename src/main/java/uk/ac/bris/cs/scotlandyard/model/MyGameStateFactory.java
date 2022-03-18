package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
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
	@Nonnull @Override
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

		// constructor for my GameState
		// constructor will be called by the build method of the outer class MyGameStateFactory
		// why private?
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			// CHECKS
			if (setup == null) throw new IllegalArgumentException("Setup is null!");
			if (remaining == null) throw new IllegalArgumentException("Remaining is null!");
			if (log == null) throw new IllegalArgumentException("Log is null!");
			if (mrX == null) throw new IllegalArgumentException("mrX is null!");
			if (detectives == null) throw new IllegalArgumentException("Detectives is null!");
			if(mrX.piece().webColour() != "#000" ) throw new IllegalArgumentException("MrX not a black piece");
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("there is no mrX!");
			if(detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
			if(setup.graph.edges().isEmpty()) throw new IllegalArgumentException("Graph is empty!");


			// check properties of each detective
			for(Player d :  detectives) {
				if(!d.isDetective()) throw new IllegalArgumentException("No detective!");
				if (d.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("detectives cant have double");
				if (d.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("detectives shouldn't have secret ticket");
			}
			// Check duplicated properties of detectives
			// O(n^2) notation can be improved using a hash table to get O(n)
			for(int i = 0; i < detectives.size(); i++) {
				for(int j = i + 1; j < detectives.size(); j++) {
					// if the next detective doest exist so break
					if(j > detectives.size()) break;
					if(detectives.get(i).location() == detectives.get(j).location()) {
						throw new IllegalArgumentException("Same location!");
					}
					if(detectives.get(i).piece() == detectives.get(j).piece()) {
						throw new IllegalArgumentException("Duplicated game pieces!");
					}
				}
			}

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

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
			if(!players.contains(piece)) return Optional.empty();

			if(piece.isMrX()) {
				// check if it has tickets has()
				return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			}
			if(piece.isDetective()) {
				for(Player player : detectives) {
					if(player.equals(piece)) {
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
			HashSet<SingleMove> sm = new HashSet<>();
			HashSet<DoubleMove> dm = new HashSet<>();
			HashSet<Move> moves = new HashSet<>();

			for(Player player : detectives) {
				sm.addAll(makeSingleMoves(setup, detectives, player, player.location()));
				dm.addAll(makeDoubleMoves(setup, detectives, player, player.location()));
			}

			moves.addAll(sm);
			moves.addAll(dm);
			return ImmutableSet.copyOf(moves);
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			return null;
		}
	}



	//-------------------- Auxiliary Functions --------------------//

	// MAKE SINGLE MOVES METHOD
	private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		HashSet<SingleMove> singleMoves = new HashSet<>();

		for(int destination : setup.graph.adjacentNodes(source)) {
			for( Player p : detectives) {
				if(p.location() == destination) continue;
			}

			for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
				if(player.has(t.requiredTicket())) {
					SingleMove m = new SingleMove(player.piece(), source, t.requiredTicket(), destination);
					singleMoves.add(m);
				}
			}
			if(player.has(Ticket.SECRET)) {
				SingleMove m = new SingleMove(player.piece(), source, Ticket.SECRET, destination);
				singleMoves.add(m);
			}
		}
		return singleMoves;
	}

	// MAKE DOUBLE MOVES METHOD
	private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

		HashSet<DoubleMove> doubleMoves = new HashSet<>();
		Set<Integer> locations = new HashSet<>();

		// store only locations
		for(Player d : detectives) locations.add(d.location());

		if((player.has(Ticket.DOUBLE))) {
			//piece, source, ticket1, destination1, ticket2, destination2
			for (int destination1 : setup.graph.adjacentNodes(source)) {
				if(locations.contains(destination1)) continue;
				for(Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
					if(player.has(t1.requiredTicket())) {
						for(int destination2 : setup.graph.adjacentNodes(destination1)) {
							if(locations.contains(destination2)) continue;
							for(Transport t2: setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
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
								}
								else if (player.has(t2.requiredTicket())) {
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

}

/*

*/


