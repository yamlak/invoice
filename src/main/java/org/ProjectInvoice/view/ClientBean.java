package org.ProjectInvoice.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.Invoice.springmvc.webapp.model.Client;
import java.util.Iterator;
import org.Invoice.springmvc.webapp.model.BillingTerms;
import org.Invoice.springmvc.webapp.model.Company;
import org.Invoice.springmvc.webapp.model.Project;

/**
 * Backing bean for Client entities.
 * <p/>
 * This class provides CRUD functionality for all Client entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class ClientBean implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Support creating and retrieving Client entities
	 */

	private Long id;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	private Client client;

	public Client getClient() {
		return this.client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	@Inject
	private Conversation conversation;

	@PersistenceContext(unitName = "Invoice-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	public String create() {

		this.conversation.begin();
		this.conversation.setTimeout(1800000L);
		return "create?faces-redirect=true";
	}

	public void retrieve() {

		if (FacesContext.getCurrentInstance().isPostback()) {
			return;
		}

		if (this.conversation.isTransient()) {
			this.conversation.begin();
			this.conversation.setTimeout(1800000L);
		}

		if (this.id == null) {
			this.client = this.example;
		} else {
			this.client = findById(getId());
		}
	}

	public Client findById(Long id) {

		return this.entityManager.find(Client.class, id);
	}

	/*
	 * Support updating and deleting Client entities
	 */

	public String update() {
		this.conversation.end();

		try {
			if (this.id == null) {
				this.entityManager.persist(this.client);
				return "search?faces-redirect=true";
			} else {
				this.entityManager.merge(this.client);
				return "view?faces-redirect=true&id=" + this.client.getId();
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	public String delete() {
		this.conversation.end();

		try {
			Client deletableEntity = findById(getId());
			Iterator<Project> iterProjects = deletableEntity.getProjects()
					.iterator();
			for (; iterProjects.hasNext();) {
				Project nextInProjects = iterProjects.next();
				nextInProjects.setClient(null);
				iterProjects.remove();
				this.entityManager.merge(nextInProjects);
			}
			Company company = deletableEntity.getCompany();
			company.getClients().remove(deletableEntity);
			deletableEntity.setCompany(null);
			this.entityManager.merge(company);
			this.entityManager.remove(deletableEntity);
			this.entityManager.flush();
			return "search?faces-redirect=true";
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	/*
	 * Support searching Client entities with pagination
	 */

	private int page;
	private long count;
	private List<Client> pageItems;

	private Client example = new Client();

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return 10;
	}

	public Client getExample() {
		return this.example;
	}

	public void setExample(Client example) {
		this.example = example;
	}

	public String search() {
		this.page = 0;
		return null;
	}

	public void paginate() {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();

		// Populate this.count

		CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
		Root<Client> root = countCriteria.from(Client.class);
		countCriteria = countCriteria.select(builder.count(root)).where(
				getSearchPredicates(root));
		this.count = this.entityManager.createQuery(countCriteria)
				.getSingleResult();

		// Populate this.pageItems

		CriteriaQuery<Client> criteria = builder.createQuery(Client.class);
		root = criteria.from(Client.class);
		TypedQuery<Client> query = this.entityManager.createQuery(criteria
				.select(root).where(getSearchPredicates(root)));
		query.setFirstResult(this.page * getPageSize()).setMaxResults(
				getPageSize());
		this.pageItems = query.getResultList();
	}

	private Predicate[] getSearchPredicates(Root<Client> root) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		Integer number = this.example.getNumber();
		if (number != null && number.intValue() != 0) {
			predicatesList.add(builder.equal(root.get("number"), number));
		}
		String name = this.example.getName();
		if (name != null && !"".equals(name)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("name")),
					'%' + name.toLowerCase() + '%'));
		}
		String contact = this.example.getContact();
		if (contact != null && !"".equals(contact)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("contact")),
					'%' + contact.toLowerCase() + '%'));
		}
		String email = this.example.getEmail();
		if (email != null && !"".equals(email)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("email")),
					'%' + email.toLowerCase() + '%'));
		}
		BillingTerms billingTerms = this.example.getBillingTerms();
		if (billingTerms != null) {
			predicatesList.add(builder.equal(root.get("billingTerms"),
					billingTerms));
		}

		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	public List<Client> getPageItems() {
		return this.pageItems;
	}

	public long getCount() {
		return this.count;
	}

	/*
	 * Support listing and POSTing back Client entities (e.g. from inside an
	 * HtmlSelectOneMenu)
	 */

	public List<Client> getAll() {

		CriteriaQuery<Client> criteria = this.entityManager
				.getCriteriaBuilder().createQuery(Client.class);
		return this.entityManager.createQuery(
				criteria.select(criteria.from(Client.class))).getResultList();
	}

	@Resource
	private SessionContext sessionContext;

	public Converter getConverter() {

		final ClientBean ejbProxy = this.sessionContext
				.getBusinessObject(ClientBean.class);

		return new Converter() {

			@Override
			public Object getAsObject(FacesContext context,
					UIComponent component, String value) {

				return ejbProxy.findById(Long.valueOf(value));
			}

			@Override
			public String getAsString(FacesContext context,
					UIComponent component, Object value) {

				if (value == null) {
					return "";
				}

				return String.valueOf(((Client) value).getId());
			}
		};
	}

	/*
	 * Support adding children to bidirectional, one-to-many tables
	 */

	private Client add = new Client();

	public Client getAdd() {
		return this.add;
	}

	public Client getAdded() {
		Client added = this.add;
		this.add = new Client();
		return added;
	}
}
