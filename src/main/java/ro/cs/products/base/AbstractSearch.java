/*
 * Copyright (C) 2016 Cosmin Cara
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.products.base;

import ro.cs.products.util.Polygon2D;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * Base class for search providers
 *
 * @author Cosmin Cara
 */
public abstract class AbstractSearch {
    protected URI url;
    protected Polygon2D aoi;
    protected double cloudFilter;
    protected String sensingStart;
    protected String sensingEnd;
    protected int relativeOrbit;
    protected Set<String> tiles;

    public AbstractSearch(String url) throws URISyntaxException {
        this.url = new URI(url);
        this.cloudFilter = Double.MAX_VALUE;
    }

    public void setSensingStart(String sensingStart) {
        this.sensingStart = sensingStart;
    }

    public void setSensingEnd(String sensingEnd) {
        this.sensingEnd = sensingEnd;
    }

    public void setAreaOfInterest(Polygon2D polygon) {
        this.aoi = polygon;
    }

    public void setClouds(double clouds) {
        this.cloudFilter = clouds;
    }

    public void setOrbit(int orbit) { this.relativeOrbit = orbit; }

    public void setTiles(Set<String> tiles) { this.tiles = tiles; }

    public Set<String> getTiles() { return this.tiles; }

    public abstract List<ProductDescriptor> execute() throws Exception;
}