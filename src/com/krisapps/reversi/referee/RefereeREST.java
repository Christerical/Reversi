package com.krisapps.reversi.referee;

import java.util.Iterator;

/**
 * Class for accessing the referee that manages games between opponents
 * @author Chris
 *
 */
public class RefereeREST extends Referee {
	/**
	 * Invite the supplied player to a game
	 * TODO hook up emails with gmail addresses
	 * @param opponent email for player or "public"
	 */
	public void invite(String opponent) {
		// TODO
	}

	public Game accept(Invite invite) {
		// TODO
		return null;
	}

	public Iterator<Invite> getInvites() {
		// TODO
		return null;
	}

	public Iterator<Game> getWaitingGames() {
		// TODO
		return null;
	}

	public Position getPosition(Game gb) {
		// TODO
		return null;
	}
}
