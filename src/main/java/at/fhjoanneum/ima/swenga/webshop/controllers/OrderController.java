package at.fhjoanneum.ima.swenga.webshop.controllers;

import at.fhjoanneum.ima.swenga.webshop.beans.LineItem;
import at.fhjoanneum.ima.swenga.webshop.controllers.util.JsfUtil;
import at.fhjoanneum.ima.swenga.webshop.controllers.util.JsfUtil.PersistAction;
import at.fhjoanneum.ima.swenga.webshop.entities.Order;
import at.fhjoanneum.ima.swenga.webshop.entities.OrderDetail;
import at.fhjoanneum.ima.swenga.webshop.facades.CustomerFacade;
import at.fhjoanneum.ima.swenga.webshop.facades.OrderFacade;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named("orderController")
@SessionScoped
public class OrderController implements Serializable {

    @EJB
    private at.fhjoanneum.ima.swenga.webshop.facades.OrderFacade ejbFacade;
    private List<Order> items = null;
    private Order selected;
    private OrderDetail newOrderDetail;
    @Inject
    private CustomerFacade customerFacade;

    public OrderController() {
    }

    public Order getSelected() {
        return selected;
    }

    public void createOrderDetail() {
        selected.getOrderDetails().add(newOrderDetail);
        newOrderDetail = null;
    }

    public void setSelected(Order selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private OrderFacade getFacade() {
        return ejbFacade;
    }

    public Order prepareCreate() {
        selected = new Order();
        initializeEmbeddableKey();
        return selected;
    }

    private void createOrderDetails(ArrayList<LineItem> lineItems) {
        for (int i = 0; i < lineItems.size(); i++) {
            int pos = i + 1;
            newOrderDetail = new OrderDetail(pos, lineItems.get(i).getQuantity(),
                    lineItems.get(i).getLinePrice(), lineItems.get(i).getProduct());
            selected.getOrderDetails().add(newOrderDetail);
        }
    }

    public String prepareOrderForm(ArrayList<LineItem> lineItems) {
        prepareCreate();
        createOrderDetails(lineItems);
        return "/pages/shop/createOrder.xhtml?faces-redirect=true";
    }

    public void initCurrentUser() throws IOException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        String email = ctx.getExternalContext().getRemoteUser();
        selected.setCustomer(customerFacade.findByEmail(email));
        selected.setDeliveryAddress(selected.getCustomer().getAddresses().iterator().next());
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("locales/Bundle").getString("OrderCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("locales/Bundle").getString("OrderUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("locales/Bundle").getString("OrderDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; 
            items = null;   
        }
    }

    public List<Order> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("locales/Bundle").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("locales/Bundle").getString("PersistenceErrorOccured"));
            }
        }
    }

    public Order getOrder(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<Order> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<Order> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = Order.class)
    public static class OrderControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            OrderController controller = (OrderController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "orderController");
            return controller.getOrder(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Order) {
                Order o = (Order) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), Order.class.getName()});
                return null;
            }
        }

    }

}
