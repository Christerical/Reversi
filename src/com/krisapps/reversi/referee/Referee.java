package com.krisapps.reversi.referee;

import java.util.Iterator;

public abstract class Referee {

	public Referee() {
		super();
	}

	/**
	 * Invite the supplied player to a game
	 * TODO hook up emails with gmail addresses
	 * @param opponent email for player or "public"
	 */
	public abstract void invite(String opponent);

	public abstract Game accept(Invite invite);

	public abstract Iterator<Invite> getInvites();

	public abstract Iterator<Game> getWaitingGames();

	public abstract Position getPosition(Game gb);
}