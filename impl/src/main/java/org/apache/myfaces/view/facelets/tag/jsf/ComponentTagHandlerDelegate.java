/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.tag.jsf;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.ValueHolder;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.Validator;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandlerDelegate;

import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;
import org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler;

/**
 *  
 * Implementation of the tag logic used in the JSF specification. 
 * 
 * @see org.apache.myfaces.view.facelets.tag.jsf.ComponentHandler
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 *
 * @since 2.0
 */
public class ComponentTagHandlerDelegate extends TagHandlerDelegate
{
    //private final static Logger log = Logger.getLogger("facelets.tag.component");
    private final static Logger log = Logger.getLogger(ComponentTagHandlerDelegate.class.getName());
    
    /**
     * The UIPanel components, which are dynamically generated to serve as a container for
     * facets with multiple non panel children, are marked with this attribute.
     * This constant is duplicate in javax.faces.webapp.UIComponentClassicTagBase
     */
    public final static String FACET_CREATED_UIPANEL_MARKER = "org.apache.myfaces.facet.createdUIPanel";
    
    private final ComponentHandler _delegate;

    private final String _componentType;

    private final TagAttribute _id;

    private final String _rendererType;

    public ComponentTagHandlerDelegate(ComponentHandler delegate)
    {
        _delegate = delegate;
        ComponentConfig delegateComponentConfig = delegate.getComponentConfig();
        _componentType = delegateComponentConfig.getComponentType();
        _rendererType = delegateComponentConfig.getRendererType();
        _id = delegate.getTagAttribute("id");
    }

    /**
     * Method handles UIComponent tree creation in accordance with the JSF 1.2 spec.
     * <ol>
     * <li>First determines this UIComponent's id by calling {@link #getId(FaceletContext) getId(FaceletContext)}.</li>
     * <li>Search the parent for an existing UIComponent of the id we just grabbed</li>
     * <li>If found, {@link #markForDeletion(UIComponent) mark} its children for deletion.</li>
     * <li>If <i>not</i> found, call {@link #createComponent(FaceletContext) createComponent}.
     * <ol>
     * <li>Only here do we apply {@link ObjectHandler#setAttributes(FaceletContext, Object) attributes}</li>
     * <li>Set the UIComponent's id</li>
     * <li>Set the RendererType of this instance</li>
     * </ol>
     * </li>
     * <li>Now apply the nextHandler, passing the UIComponent we've created/found.</li>
     * <li>Now add the UIComponent to the passed parent</li>
     * <li>Lastly, if the UIComponent already existed (found), then {@link #finalizeForDeletion(UIComponent) finalize}
     * for deletion.</li>
     * </ol>
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, javax.faces.component.UIComponent)
     * 
     * @throws TagException
     *             if the UIComponent parent is null
     */
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        // make sure our parent is not null
        if (parent == null)
        {
            throw new TagException(_delegate.getTag(), "Parent UIComponent was null");
        }
        
        FacesContext facesContext = ctx.getFacesContext();

        // possible facet scoped
        String facetName = this.getFacetName(ctx, parent);

        // our id
        String id = ctx.generateUniqueId(_delegate.getTagId());

        // Cast to use UniqueIdVendor stuff
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
                
        // grab our component
        UIComponent c = ComponentSupport.findChildByTagId(parent, id);
        boolean componentFound = false;
        if (c != null)
        {
            componentFound = true;
            // mark all children for cleaning
            if (log.isLoggable(Level.FINE))
            {
                log.fine(_delegate.getTag() + " Component[" + id + "] Found, marking children for cleanup");
            }
            ComponentSupport.markForDeletion(c);
        }
        else
        {
            c = this.createComponent(ctx);
            if (log.isLoggable(Level.FINE))
            {
                log.fine(_delegate.getTag() + " Component[" + id + "] Created: " + c.getClass().getName());
            }
            
            _delegate.setAttributes(ctx, c);

            // mark it owned by a facelet instance
            c.getAttributes().put(ComponentSupport.MARK_CREATED, id);

            if (facesContext.isProjectStage(ProjectStage.Development))
            {
                c.getAttributes().put(UIComponent.VIEW_LOCATION_KEY,
                        _delegate.getTag().getLocation());
            }

            // assign our unique id
            if (this._id != null)
            {
                c.setId(this._id.getValue(ctx));
            }
            else
            {
                UniqueIdVendor uniqueIdVendor = actx.getUniqueIdVendorFromStack();
                if (uniqueIdVendor == null)
                {
                    uniqueIdVendor = facesContext.getViewRoot();
                }
                if (uniqueIdVendor != null)
                {
                    // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
                    // and call createUniqueId()
                    String uid = uniqueIdVendor.createUniqueId(facesContext, id);
                    c.setId(uid);
                }
            }

            if (this._rendererType != null)
            {
                c.setRendererType(this._rendererType);
            }

            // hook method
            _delegate.onComponentCreated(ctx, c, parent);
        }
        c.pushComponentToEL(facesContext, c);

        if (c instanceof UniqueIdVendor)
        {
            actx.pushUniqueIdVendorToStack((UniqueIdVendor)c);
        }
        // first allow c to get populated
        _delegate.applyNextHandler(ctx, c);

        // finish cleaning up orphaned children
        if (componentFound)
        {
            ComponentSupport.finalizeForDeletion(c);

            if (facetName == null)
            {
                parent.getChildren().remove(c);
            }
            else
            {
                UIComponent facet = parent.getFacet(facetName);
                if (Boolean.TRUE.equals(facet.getAttributes().get(FACET_CREATED_UIPANEL_MARKER)))
                {
                    facet.getChildren().remove(c);
                }
                else
                {
                    parent.getFacets().remove(facetName);
                }
            }
        }

        if (c instanceof ClientBehaviorHolder && !UIComponent.isCompositeComponent(c))
        {
            Iterator<AjaxHandler> it = actx.getAjaxHandlers();
            if (it != null)
            {
                while(it.hasNext())
                {
                    it.next().applyAttachedObject(facesContext, c);
                }
            }
        }
        
        if (c instanceof EditableValueHolder)
        {
            // add default validators here, because this feature 
            // is only available in facelets (see MYFACES-2362 for details)
            addDefaultValidators(facesContext, actx, (EditableValueHolder) c);
        }
        
        _delegate.onComponentPopulated(ctx, c, parent);

        // add to the tree afterwards
        // this allows children to determine if it's
        // been part of the tree or not yet
        if (facetName == null)
        {
            parent.getChildren().add(c);
        }
        else
        {
            // facets now can have multiple children and the direct
            // child of a facet is always an UIPanel (since 2.0)
            UIComponent facet = parent.getFacets().get(facetName);
            boolean facetChanged = false;
            
            if (facet == null)
            {
                // if our component is an instance of UIPanel, use it
                if (c instanceof UIPanel)
                {
                    facet = c;
                }
                else
                {
                    // create a new UIPanel and add c as child
                    facet = createFacetUIPanel(facesContext);
                    facet.getChildren().add(c);
                }
                facetChanged = true;
            }
            else if (!(facet instanceof UIPanel))
            {
                // there is a facet, but it is not an instance of UIPanel
                UIComponent child = facet;
                facet = createFacetUIPanel(facesContext);
                facet.getChildren().add(child);
                facet.getChildren().add(c);
                facetChanged = true;
            }
            else
            {
                // we have a facet, which is an instance of UIPanel at this point
                // check if it is a facet marked UIPanel
                if (Boolean.TRUE.equals(facet.getAttributes().get(FACET_CREATED_UIPANEL_MARKER)))
                {
                    facet.getChildren().add(c);
                }
                else
                {
                    // the facet is an instance of UIPanel, but it is not marked,
                    // so we have to create a new UIPanel and store this one in it
                    UIComponent oldPanel = facet;
                    facet = createFacetUIPanel(facesContext);
                    facet.getChildren().add(oldPanel);
                    facet.getChildren().add(c);
                    facetChanged = true;
                }
            }
            
            if (facetChanged)
            {
                parent.getFacets().put(facetName, facet);
            }
        }
        
        if (c instanceof UniqueIdVendor)
        {
            actx.popUniqueIdVendorToStack();
        }

        c.popComponentFromEL(facesContext);
        
        if (facesContext.getAttributes().containsKey(
                FaceletViewDeclarationLanguage.MARK_INITIAL_STATE_KEY))
        {
            //Call it only if we are using partial state saving
            c.markInitialState();
        }
    }
    
    /**
     * Return the Facet name we are scoped in, otherwise null
     * 
     * @param ctx
     * @return
     */
    protected final String getFacetName(FaceletContext ctx, UIComponent parent)
    {
        return (String) parent.getAttributes().get(FacetHandler.KEY);
    }

    /**
     * If the binding attribute was specified, use that in conjuction with our componentType String variable to call
     * createComponent on the Application, otherwise just pass the componentType String. <p /> If the binding was used,
     * then set the ValueExpression "binding" on the created UIComponent.
     * 
     * @see Application#createComponent(javax.faces.el.ValueBinding, javax.faces.context.FacesContext, java.lang.String)
     * @see Application#createComponent(java.lang.String)
     * @param ctx
     *            FaceletContext to use in creating a component
     * @return
     */
    protected UIComponent createComponent(FaceletContext ctx)
    {
        if (_delegate instanceof ComponentBuilderHandler)
        {
            // the call to Application.createComponent(FacesContext, Resource)
            // is delegated because we don't have here the required Resource instance
            return ((ComponentBuilderHandler) _delegate).createComponent(ctx);
        }
        UIComponent c = null;
        FacesContext faces = ctx.getFacesContext();
        Application app = faces.getApplication();
        if (_delegate.getBinding() != null)
        {
            ValueExpression ve = _delegate.getBinding().getValueExpression(ctx, Object.class);
            if (this._rendererType == null)
            {
                c = app.createComponent(ve, faces, this._componentType);
            }
            else
            {
                c = app.createComponent(ve, faces, this._componentType, this._rendererType);
            }
            if (c != null)
            {
                c.setValueExpression("binding", ve);
            }
        }
        else
        {
            if (this._rendererType == null)
            {
                c = app.createComponent(this._componentType);
            }
            else
            {
                c = app.createComponent(faces, this._componentType, this._rendererType);
            }
        }
        return c;
    }

    /**
     * If the id TagAttribute was specified, get it's value, otherwise generate a unique id from our tagId.
     * 
     * @see TagAttribute#getValue(FaceletContext)
     * @param ctx
     *            FaceletContext to use
     * @return what should be a unique Id
     */
    protected String getId(FaceletContext ctx)
    {
        if (this._id != null)
        {
            return this._id.getValue(ctx);
        }
        return ctx.generateUniqueId(_delegate.getTagId());
    }

    @Override
    public MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset m = new MetaRulesetImpl(_delegate.getTag(), type);
        // ignore standard component attributes
        m.ignore("binding").ignore("id");

        // add auto wiring for attributes
        m.addRule(ComponentRule.Instance);

        // if it's an ActionSource
        if (ActionSource.class.isAssignableFrom(type))
        {
            m.addRule(ActionSourceRule.Instance);
        }

        // if it's a ValueHolder
        if (ValueHolder.class.isAssignableFrom(type))
        {
            m.addRule(ValueHolderRule.Instance);

            // if it's an EditableValueHolder
            if (EditableValueHolder.class.isAssignableFrom(type))
            {
                m.ignore("submittedValue");
                m.ignore("valid");
                m.addRule(EditableValueHolderRule.Instance);
            }
        }
        
        return m;
    }
    
    /**
     * Add the default Validators to the component.
     *
     * @param context The FacesContext.
     * @param actx the AbstractFaceletContext
     * @param component The EditableValueHolder to which the validators should be added
     */
    private void addDefaultValidators(FacesContext context, AbstractFaceletContext actx,
                                      EditableValueHolder component)
    {
        Application application = context.getApplication();
        Map<String, String> defaultValidators = application.getDefaultValidatorInfo();
        if (defaultValidators != null && defaultValidators.size() != 0)
        {
            Set<Map.Entry<String, String>> defaultValidatorInfoSet = defaultValidators.entrySet();
            for (Map.Entry<String, String> entry : defaultValidatorInfoSet)
            {
                String validatorId = entry.getKey();
                String validatorClassName = entry.getValue();
                
                if (shouldAddDefaultValidator(validatorId, validatorClassName, context, actx, component))
                {
                    Validator validator = null;
                    boolean created = false;
                    // check if the validator is already registered for the given component
                    for (Validator v : component.getValidators())
                    {
                        if (v.getClass().getName().equals(validatorClassName))
                        {
                            // found
                            validator = v;
                            break;
                        }
                    }
                    if (validator == null)
                    {
                        // create it
                        validator = application.createValidator(validatorId);
                        created = true;
                    }
                    
                    // special things to do for a BeanValidator
                    if (validator instanceof BeanValidator)
                    {
                        BeanValidator beanValidator = (BeanValidator) validator;
                        
                        // check the validationGroups
                        String validationGroups =  beanValidator.getValidationGroups();
                        if (validationGroups == null 
                                || validationGroups.matches(BeanValidator.EMPTY_VALIDATION_GROUPS_PATTERN))
                        {
                            // no validationGroups available
                            // --> get the validationGroups from the stack
                            String stackGroup = actx.getFirstValidationGroupFromStack();
                            if (stackGroup != null)
                            {
                                validationGroups = stackGroup;
                            }
                            else
                            {
                                // no validationGroups on the stack
                                // --> set the default validationGroup
                                validationGroups = javax.validation.groups.Default.class.getName();
                            }
                            beanValidator.setValidationGroups(validationGroups);
                        }
                    }
                    
                    if (created)
                    {
                        // add the validator to the component
                        component.addValidator(validator);
                    }
                }
            }
        }
    }

    /**
     * Determine if the default Validator with the given validatorId should be added.
     *
     * @param validatorId The validatorId.
     * @param validatorClassName The class name of the validator.
     * @param context The FacesContext.
     * @param actx the AbstractFaceletContext
     * @param component The EditableValueHolder to which the validator should be added.
     * @return true if the Validator should be added, false otherwise.
     */
    @SuppressWarnings("unchecked")
    private boolean shouldAddDefaultValidator(String validatorId, String validatorClassName,
                                              FacesContext context, AbstractFaceletContext actx,
                                              EditableValueHolder component)
    {
        // check if the validatorId is on the exclusion list on the component
        List<String> exclusionList 
                = (List<String>) ((UIComponent) component).getAttributes()
                        .get(ValidatorTagHandlerDelegate.VALIDATOR_ID_EXCLUSION_LIST_KEY);
        if (exclusionList != null)
        {
            for (String excludedId : exclusionList)
            {
                if (excludedId.equals(validatorId))
                {
                    return false;
                }
            }
        }
        
        // check if the validatorId is on the exclusion list on the stack
        Iterator<String> it = actx.getExcludedValidatorIds();
        if (it != null)
        {            
            while (it.hasNext())
            {
                String excludedId = it.next();
                if (excludedId.equals(validatorId))
                {
                    return false;
                }
            }
        }
        
        // Some extra rules are required for Bean Validation.
        if (validatorId.equals(BeanValidator.VALIDATOR_ID))
        {
            if (!ExternalSpecifications.isBeanValidationAvailable)
            {
                return false;
            }
            ExternalContext externalContext = context.getExternalContext();
            String disabled = externalContext.getInitParameter(BeanValidator.DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME);
            if (disabled != null && disabled.toLowerCase().equals("true"))
            {
                return false;
            }
        }

        // By default, all default validators should be added
        return true;
    }
    
    /**
     * Create a new UIPanel for the use as a dynamically 
     * created container for multiple children in a facet.
     * Duplicate in javax.faces.webapp.UIComponentClassicTagBase.
     * @param facesContext
     * @return
     */
    private UIComponent createFacetUIPanel(FacesContext facesContext)
    {
        UIComponent panel = facesContext.getApplication().createComponent(UIPanel.COMPONENT_TYPE);
        panel.setId(facesContext.getViewRoot().createUniqueId());
        panel.getAttributes().put(FACET_CREATED_UIPANEL_MARKER, Boolean.TRUE);
        return panel;
    }

}
