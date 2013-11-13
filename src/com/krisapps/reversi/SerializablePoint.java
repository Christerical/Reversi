package com.krisapps.reversi;

import java.io.Serializable;

import android.graphics.Point;

/**
 * Point is not marked as serializable!
 * 
 * @author Chris
 *
 */
public class SerializablePoint extends Point implements Serializable {

	public SerializablePoint() {
		super();
	}

	public SerializablePoint(int x, int y) {
		super(x, y);
	}

	public SerializablePoint(Point src) {
		super(src);
	}

}
