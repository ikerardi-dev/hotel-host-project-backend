package com.hotelhost.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "beds")
public class Bed {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "apartment_id")
	private Apartment apartment;

	@Enumerated(EnumType.STRING)
	private BedType type;

	private Integer quantity;

	protected Bed() {
	}

	public Long getId() {
		return id;
	}

	public Apartment getApartment() {
		return apartment;
	}

	public BedType getType() {
		return type;
	}

	public Integer getQuantity() {
		return quantity;
	}
}
