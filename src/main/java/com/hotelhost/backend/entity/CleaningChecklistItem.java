package com.hotelhost.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cleaning_checklist_items")
public class CleaningChecklistItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cleaning_id")
	private Cleaning cleaning;

	private String label;

	private boolean done;

	private Integer position;

	protected CleaningChecklistItem() {
	}

	public CleaningChecklistItem(String label, int position) {
		this.label = label;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Cleaning getCleaning() {
		return cleaning;
	}

	void setCleaning(Cleaning cleaning) {
		this.cleaning = cleaning;
	}

	public String getLabel() {
		return label;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public Integer getPosition() {
		return position;
	}
}
