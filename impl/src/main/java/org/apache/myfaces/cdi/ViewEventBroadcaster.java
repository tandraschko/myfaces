package org.apache.myfaces.cdi;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.annotation.View;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import org.apache.myfaces.cdi.util.CDIUtils;

public class ViewEventBroadcaster implements SystemEventListener
{
    public ViewEventBroadcaster()
    {
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        return source instanceof UIViewRoot;
    }

    @Override
    public void processEvent(SystemEvent e) throws AbortProcessingException
    {
        BeanManager beanManager = CDIUtils.getBeanManager(e.getFacesContext());

        Event<Object> event = beanManager.getEvent();
        event.fire(e);

        UIViewRoot view = (UIViewRoot) e.getSource();
        event.select(View.Literal.of(view.getViewId())).fire(e);
    }
}