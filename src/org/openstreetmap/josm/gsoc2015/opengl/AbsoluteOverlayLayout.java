package org.openstreetmap.josm.gsoc2015.opengl;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * This is a layout that positions one component with full size and the rest of
 * the components as if there was no layout.
 *
 * @author Michael Zangl
 *
 */
public class AbsoluteOverlayLayout implements LayoutManager {

	private final Component fullSizeComponent;

	public AbsoluteOverlayLayout(Component fullSizeComponent) {
		this.fullSizeComponent = fullSizeComponent;

	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	@Override
	public void layoutContainer(Container parent) {
		for (final Component c : parent.getComponents()) {
			if (c == fullSizeComponent) {
				c.setBounds(new Rectangle(parent.getSize()));
			}
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return fullSizeComponent.getMinimumSize();
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return fullSizeComponent.getPreferredSize();
	}

	@Override
	public void removeLayoutComponent(Component comp) {
	}

}
