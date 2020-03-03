/*
 * $Id: Viewport.java,v 1.2 2006/02/27 18:55:52 weiju Exp $
 * 
 * Created on 2006/02/24
 * Copyright 2005-2006 by Wei-ju Wu
 *
 * This file is part of The Z-machine Preservation Project (ZMPP).
 *
 * ZMPP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * ZMPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ZMPP; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.zmpp.swingui;

import org.zmpp.vm.ScreenModel;

public interface Viewport extends ScreenModel {

  CursorWindow getCurrentWindow();
  int getDefaultBackground();
  int getDefaultForeground();
  LineEditor getLineEditor();
  Canvas getCanvas();
}
