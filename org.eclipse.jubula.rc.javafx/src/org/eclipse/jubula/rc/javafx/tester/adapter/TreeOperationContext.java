/*******************************************************************************
 * Copyright (c) 2013 BREDEX GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BREDEX GmbH - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.jubula.rc.javafx.tester.adapter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jubula.rc.common.driver.ClickOptions;
import org.eclipse.jubula.rc.common.driver.IEventThreadQueuer;
import org.eclipse.jubula.rc.common.driver.IRobot;
import org.eclipse.jubula.rc.common.exception.StepExecutionException;
import org.eclipse.jubula.rc.common.implclasses.tree.AbstractTreeOperationContext;
import org.eclipse.jubula.rc.common.logger.AutServerLogger;
import org.eclipse.jubula.rc.common.util.SelectionUtil;
import org.eclipse.jubula.rc.javafx.driver.EventThreadQueuerJavaFXImpl;
import org.eclipse.jubula.rc.javafx.listener.ComponentHandler;
import org.eclipse.jubula.rc.javafx.util.Rounding;

/**
 * This context holds the tree and supports access to the Robot. It also
 * implements some general operations on trees.
 *
 * @author BREDEX GmbH
 * @created 19.11.2013
 */
public class TreeOperationContext extends AbstractTreeOperationContext {

    /** The AUT Server logger. */
    private static AutServerLogger log = new AutServerLogger(
            TreeOperationContext.class);

    /**
     * Creates a new instance. The JTree must have a tree model.
     *
     * @param queuer
     *            The queuer
     * @param robot
     *            The Robot
     * @param tree
     *            The tree
     */
    public TreeOperationContext(IEventThreadQueuer queuer, IRobot robot,
            TreeView<?> tree) {
        super(queuer, robot, tree);
        Validate.notNull(tree.getRoot());
    }

    /**
     * {@inheritDoc}
     *
     * @param row
     *            Not used!
     */
    @Override
    protected String convertValueToText(final Object node, final int row)
        throws StepExecutionException {
        String result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "convertValueToText", new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        if (item != null) {
                            Object val = item.getValue();
                            if (val != null) {
                                return val.toString();
                            }
                        }
                        return null;
                    }
                });
        return result;
    }

    @Override
    public Collection<String> getNodeTextList(Object node) {
        List<String> res = new ArrayList<String>();
        int rowNotUsed = 0;
        String valText = convertValueToText(node, rowNotUsed);
        if (valText != null) {
            res.add(valText);
        }
        String rendText = getRenderedText(node);
        if (rendText != null) {
            res.add(rendText);
        }
        return res;
    }

    @Override
    public String getRenderedText(final Object node)
        throws StepExecutionException {
        String result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getRenderedText", new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        List<Object> tCells = ComponentHandler
                                .getInstancesOfType(TreeCell.class);
                        for (Object o : tCells) {
                            TreeCell<?> cell = (TreeCell<?>) o;
                            TreeItem<?> item = cell.getTreeItem();
                            if (item != null && item.equals(node)) {
                                return cell.getText();
                            }
                        }
                        return null;
                    }
                });

        return result;
    }

    @Override
    public boolean isVisible(final Object node) {
        boolean result = EventThreadQueuerJavaFXImpl.invokeAndWait("isVisible",
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        return item.isExpanded()
                                && ((TreeView<?>) getTree()).isVisible();
                    }
                });

        return result;
    }

    @Override
    public Rectangle getVisibleRowBounds(final Rectangle rowBounds) {
        Rectangle result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getVisibleRowBounds", new Callable<Rectangle>() {

                    @Override
                    public Rectangle call() throws Exception {
                        TreeView<?> tree = ((TreeView<?>) getTree());
                        // Update the layout coordinates otherwise
                        // we would get old position values
                        tree.layout();
                        Rectangle visibleTreeBounds = new Rectangle(0, 0,
                                Rounding.round(tree.getWidth()), Rounding
                                        .round(tree.getHeight()));
                        return rowBounds.intersection(visibleTreeBounds);
                    }
                });
        return result;

    }

    @Override
    public boolean isExpanded(final Object node) {
        boolean result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "isExpanded", new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        return item.isExpanded();
                    }
                });

        return result;
    }

    @Override
    public void scrollNodeToVisible(final Object node) {
        EventThreadQueuerJavaFXImpl.invokeAndWait("scrollNodeToVisible",
                new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        int index = ((TreeView<?>) getTree())
                                .getRow((TreeItem) node);
                        ((TreeView<?>) getTree()).scrollTo(index);
                        // Update the layout coordinates otherwise
                        // we would get old position values
                        ((TreeView<?>) getTree()).layout();
                        return null;
                    }
                });
    }

    @Override
    public void clickNode(Object node, ClickOptions clickOps) {
        scrollNodeToVisible(node);
        Rectangle rowBounds = getNodeBounds(node);
        Rectangle visibleRowBounds = getVisibleRowBounds(rowBounds);
        getRobot().click(getTree(), visibleRowBounds, clickOps);
    }

    @Override
    public void expandNode(final Object node) {
        scrollNodeToVisible(node);
        Object result = EventThreadQueuerJavaFXImpl.invokeAndWait("expandNode",
                new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        List<Object> tCells = ComponentHandler
                                .getInstancesOfType(TreeCell.class);
                        for (Object o : tCells) {
                            TreeCell<?> cell = (TreeCell<?>) o;
                            TreeItem<?> item = cell.getTreeItem();
                            if (item != null && item.equals(node)
                                    && !item.isExpanded()) {
                                TreeView<?> tree = ((TreeView<?>) getTree());
                                // Update the layout coordinates otherwise
                                // we would get old position values
                                tree.layout();

                                Node n = cell.getDisclosureNode();
                                Bounds b = n.getBoundsInParent();

                                Rectangle cBounds = new Rectangle(
                                        Rounding.round(b.getMinX()),
                                        Rounding.round(b.getMinY()),
                                        Rounding.round(b.getWidth()
                                                - cell.getGraphicTextGap()),
                                        Rounding.round(cell.getHeight()));
                                return n;
                            }
                        }
                        return null;
                    }
                });
        if (result != null) {
            getRobot().click(result, null,
                    ClickOptions.create().setClickCount(1).setMouseButton(1));
        }
        EventThreadQueuerJavaFXImpl.invokeAndWait("expandNodeCheckIfExpanded",
                new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        if (!((TreeView<?>) getTree()).isDisabled()
                                && !item.isExpanded()) {
                            log.warn("Expand node fallback used for: "
                                    + item.getValue());

                            item.setExpanded(true);
                        }
                        return null;
                    }
                });
    }

    @Override
    public void collapseNode(final Object node) {
        scrollNodeToVisible(node);
        Object result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "collapseNode", new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        List<Object> tCells = ComponentHandler
                                .getInstancesOfType(TreeCell.class);
                        for (Object o : tCells) {
                            TreeCell<?> cell = (TreeCell<?>) o;
                            TreeItem<?> item = cell.getTreeItem();
                            if (item != null && item.equals(node)
                                    && item.isExpanded()) {
                                TreeView<?> tree = ((TreeView<?>) getTree());
                                // Update the layout coordinates otherwise
                                // we would get old position values
                                tree.layout();

                                Node n = cell.getDisclosureNode();
                                Bounds b = n.getBoundsInParent();

                                Rectangle cBounds = new Rectangle(
                                        Rounding.round(b.getMinX()),
                                        Rounding.round(b.getMinY()),
                                        Rounding.round(b.getWidth()
                                                - cell.getGraphicTextGap()),
                                        Rounding.round(cell.getHeight()));
                                return n;
                            }
                        }
                        return null;
                    }
                });
        if (result != null) {
            getRobot().click(result, null,
                    ClickOptions.create().setClickCount(1).setMouseButton(1));
        }
        EventThreadQueuerJavaFXImpl.invokeAndWait(
                "collapseNodeCheckIfCollapsed", new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        if (!((TreeView<?>) getTree()).isDisabled()
                                && item.isExpanded()) {
                            log.warn("Collapse node fallback used for: "
                                    + item.getValue());

                            item.setExpanded(false);
                        }
                        return null;
                    }
                });
    }

    @Override
    public Object getSelectedNode() {
        Object result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getSelectedNode", new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {

                        return ((TreeView<?>) getTree()).getSelectionModel()
                                .getSelectedItem();
                    }
                });
        if (result != null) {
            SelectionUtil.validateSelection(new Object[] { result });
        } else {
            SelectionUtil.validateSelection(new Object[] {});
        }
        return result;
    }

    @Override
    public Object[] getSelectedNodes() {
        Object[] result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getSelectedNode", new Callable<Object[]>() {

                    @Override
                    public Object[] call() throws Exception {

                        return ((TreeView<?>) getTree()).getSelectionModel()
                                .getSelectedItems().toArray();
                    }
                });
        SelectionUtil.validateSelection(result);
        return result;
    }

    @Override
    public Object[] getRootNodes() {
        Object[] result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getRootNodes", new Callable<Object[]>() {

                    @Override
                    public Object[] call() throws Exception {
                        TreeView<?> tree = (TreeView<?>) getTree();

                        // If the root is visible, just return that.
                        if (tree.showRootProperty().getValue()) {
                            return new Object[] { tree.getRoot() };
                        }

                        // If the root is not visible, return all direct
                        // children of the
                        // non-visible root.
                        return getChildren(tree.getRoot());
                    }
                });
        return result;
    }

    @Override
    public Object getParent(final Object child) {
        Object result = EventThreadQueuerJavaFXImpl.invokeAndWait("getParent",
                new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {

                        return ((TreeItem<?>) child).getParent();
                    }
                });

        return result;
    }

    @Override
    public Object getChild(final Object parent, final int index) {
        Object result = EventThreadQueuerJavaFXImpl.invokeAndWait("getChild",
                new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {

                        return ((TreeItem<?>) parent).getChildren().get(index);
                    }
                });

        return result;
    }

    @Override
    public int getNumberOfChildren(final Object parent) {
        Integer result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getNumberOfChildren", new Callable<Integer>() {

                    @Override
                    public Integer call() throws Exception {

                        return ((TreeItem<?>) parent).getChildren().size();
                    }
                });

        return result;
    }

    @Override
    public boolean isLeaf(final Object node) {
        boolean result = EventThreadQueuerJavaFXImpl.invokeAndWait("isLeaf",
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        TreeItem<?> item = (TreeItem<?>) node;
                        return item.isLeaf();
                    }
                });

        return result;
    }

    @Override
    public Object[] getChildren(final Object parent) {
        Object[] result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getSelectedNode", new Callable<Object[]>() {

                    @Override
                    public Object[] call() throws Exception {

                        return ((TreeItem<?>) parent).getChildren().toArray();
                    }
                });

        return result;
    }

    @Override
    public Rectangle getNodeBounds(final Object node) {
        Rectangle result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getNodeBounds", new Callable<Rectangle>() {
                    @Override
                    public Rectangle call() throws Exception {
                        List<Object> tCells = ComponentHandler
                                .getInstancesOfType(TreeCell.class);
                        for (Object o : tCells) {
                            TreeCell cell = (TreeCell<?>) o;
                            TreeItem<?> item = cell.getTreeItem();
                            TreeView<?> tree = (TreeView<?>) getTree();
                            if ((item != null && item.equals(node))) {
                                // Update the layout coordinates otherwise
                                // we would get old position values
                                tree.layout();

                                Point2D posTree = tree.localToScreen(0, 0);
                                Point2D posCell = cell.localToScreen(0, 0);
                                Bounds b = cell.getBoundsInParent();
                                Rectangle cBounds = new Rectangle(
                                        Math.abs(Rounding.round((posCell.getX()
                                                - posTree.getX()))),
                                        Math.abs(Rounding.round((posCell.getY()
                                                - posTree.getY()))),
                                                Rounding.round(b.getWidth()),
                                                Rounding.round(b.getHeight()));
                                return cBounds;
                            }
                        }
                        return null;
                    }
                });
        return result;
    }

    @Override
    public int getIndexOfChild(final Object parent, final Object child) {
        Integer result = EventThreadQueuerJavaFXImpl.invokeAndWait(
                "getIndexOfChild", new Callable<Integer>() {

                    @Override
                    public Integer call() throws Exception {
                        if (parent == null) {
                            Object[] rootNodes = getRootNodes();
                            for (int i = 0; i < rootNodes.length; i++) {
                                if (ObjectUtils.equals(rootNodes[i], child)) {
                                    return i;
                                }
                            }

                            return -1;
                        }
                        List<?> children = ((TreeItem<?>) parent).getChildren();
                        if (children.contains(child)) {
                            return children.indexOf(child);
                        }
                        return -1;
                    }
                });
        return result;
    }

}