package org.apache.myfaces.cdi;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.event.*;
import org.apache.myfaces.cdi.util.CDIUtils;

public class PhaseBroadcaster  implements PhaseListener
{
    @Override
    public void beforePhase(PhaseEvent phaseEvent)
    {
        BeanManager beanManager = CDIUtils.getBeanManager(phaseEvent.getFacesContext());
        if (beanManager == null)
        {
            return;
        }

        Event<Object> event = beanManager.getEvent();

        event.select(BeforePhase.Literal.of(phaseEvent.getPhaseId())).fire(event);

        event.select(BeforePhase.Literal.of(PhaseId.ANY_PHASE)).fire(event);
    }

    @Override
    public void afterPhase(PhaseEvent phaseEvent)
    {
        BeanManager beanManager = CDIUtils.getBeanManager(phaseEvent.getFacesContext());
        if (beanManager == null)
        {
            return;
        }

        Event<Object> event = beanManager.getEvent();

        event.select(AfterPhase.Literal.of(phaseEvent.getPhaseId())).fire(event);

        event.select(AfterPhase.Literal.of(PhaseId.ANY_PHASE)).fire(event);
    }

    @Override
    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }
}