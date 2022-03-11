package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */

// CLASS
public final class MyGameStateFactory implements Factory<GameState> {

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


			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;


			// CHECKS
			if(mrX.piece().webColour() != "#000" ) throw new IllegalArgumentException("MrX not a black piece");
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("there is no mrX!");
			if(detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
			if (Objects.equals(setup.graph.hashCode(), 0)) throw new IllegalArgumentException("Graph is empty!");

			// Time complexity can be improved using a hashmap
			for(int i = 0; i < detectives.size(); i++) {
				if(!detectives.get(i).isDetective()) throw new IllegalArgumentException("No detective!");
				if (detectives.get(i).has(ScotlandYard.Ticket.DOUBLE)) {
					throw new IllegalArgumentException("detectives cant have double");
				}
				for(int j = i + 1; j < detectives.size(); j++) {
					if(j > detectives.size()) break;
					if(detectives.get(i).location() == detectives.get(j).location()) {
						throw new IllegalArgumentException("Same location!");
					}
					if(detectives.get(i).piece() == detectives.get(j).piece()) {
						throw new IllegalArgumentException("Duplicated game pieces!");
					}
				}
			}

			System.out.println("Hola");



		}


		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			return remaining;
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			// For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
			// otherwise, return Optional.empty();
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
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
			return moves;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			return null;
		}
	}

	// BUILD METHOD
	@Nonnull @Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		// build return a new instance of GameState
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}


