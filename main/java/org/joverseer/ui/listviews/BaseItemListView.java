package org.joverseer.ui.listviews;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.joverseer.domain.IHasMapLocation;
import org.joverseer.support.GameHolder;
import org.joverseer.ui.LifecycleEventsEnum;
import org.joverseer.ui.support.JOverseerEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.application.PageComponentContext;
import org.springframework.richclient.application.support.AbstractView;
import org.springframework.richclient.command.support.AbstractActionCommandExecutor;
import org.springframework.richclient.table.BeanTableModel;
import org.springframework.richclient.table.SortableTableModel;
import org.springframework.richclient.table.TableUtils;


public abstract class BaseItemListView extends AbstractView implements ApplicationListener, MouseListener {
    protected BeanTableModel tableModel;

    protected JTable table;
    protected Class tableModelClass;
    protected SelectHexCommandExecutor selectHexCommandExecutor = new SelectHexCommandExecutor();

    public BaseItemListView(Class tableModelClass) {
        this.tableModelClass = tableModelClass;
    }
    
    protected abstract void setItems();
 
    protected abstract int[] columnWidths();
    
    protected void registerLocalCommandExecutors(PageComponentContext pageComponentContext) {
        pageComponentContext.register("selectHexCommand", selectHexCommandExecutor);
        selectHexCommandExecutor.setEnabled(GameHolder.hasInitializedGame());
    }
    
    private class SelectHexCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int idx = ((SortableTableModel)table.getModel()).convertSortedIndexToDataIndex(row);
                if (idx >= tableModel.getRowCount()) return;
                try {
                    Object obj = tableModel.getRow(idx);
                    if (!IHasMapLocation.class.isInstance(obj)) return;
                    IHasMapLocation selectedItem = (IHasMapLocation)obj;
                    Point selectedHex = new Point(selectedItem.getX(), selectedItem.getY());
                    Application.instance().getApplicationContext().publishEvent(
                            new JOverseerEvent(LifecycleEventsEnum.SelectedHexChangedEvent.toString(), selectedHex, this));
                }
                catch (Exception exc) {
                    // do nothing
                }
            }
        }
    }
    
    protected JComponent createControl() {
        return createControlImpl();
    }
    protected JComponent createControlImpl() {

        // fetch the messageSource instance from the application context
        MessageSource messageSource = (MessageSource) getApplicationContext().getBean("messageSource");

        // create the table model
        try {
            tableModel = (BeanTableModel)tableModelClass.getConstructor(new Class[]{MessageSource.class}).newInstance(new Object[]{messageSource});
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        setItems();

        // create the JTable instance
        table = TableUtils.createStandardSortableTable(tableModel);
        org.joverseer.ui.support.TableUtils.setTableColumnWidths(table, columnWidths());

        table.addMouseListener(this);
        JScrollPane scrollPane = new JScrollPane(table);
        //scrollPane.getViewport().setOpaque(true);
        //scrollPane.getViewport().setBackground(table.getBackground());
        return scrollPane;
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JOverseerEvent) {
            JOverseerEvent e = (JOverseerEvent)applicationEvent;
            if (e.getEventType().equals(LifecycleEventsEnum.SelectedTurnChangedEvent.toString())) {
                setItems();
            } else if (e.getEventType().equals(LifecycleEventsEnum.SelectedHexChangedEvent.toString())) {
                setItems();
            } else if (e.getEventType().equals(LifecycleEventsEnum.GameChangedEvent.toString())) {
                setItems();
            }
        }
    }
    
    public void showContextMenu() {
        JPopupMenu pm = getPopupMenu();
        if (pm == null) return;
        JComponent cmp = (JComponent)table;
        pm.show(cmp, 0, cmp.getHeight());
    };
    
    public JPopupMenu getPopupMenu() {
        return null;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == 1) {
            selectHexCommandExecutor.execute();
        }
        if (e.getClickCount() == 1 && e.getButton() == 3) {
            showContextMenu();
        }
    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}