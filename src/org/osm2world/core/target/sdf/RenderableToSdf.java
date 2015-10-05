package org.osm2world.core.target.sdf;

import org.osm2world.core.target.common.RenderableToPrimitiveTarget;

public interface RenderableToSdf extends RenderableToPrimitiveTarget {

	public void renderTo(SdfTarget target);

}
