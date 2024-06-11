package org.apache.myfaces.cdi;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import org.apache.myfaces.cdi.util.CDIUtils;

/**
 * Broadcasts Faces system events to CDI.
 */
public class SystemEventBroadcaster implements SystemEventListener
{
    public SystemEventBroadcaster()
    {
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        return true;
    }

    @Override
    public void processEvent(SystemEvent e) throws AbortProcessingException
    {
        BeanManager beanManager = CDIUtils.getBeanManager(e.getFacesContext());
        beanManager.getEvent().fire(e);
    }
}