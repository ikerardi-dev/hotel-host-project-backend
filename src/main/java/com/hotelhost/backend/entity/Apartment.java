package com.hotelhost.backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "apartments")
public class Apartment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "building_id")
	private Building building;

	private String code;

	private Integer capacity;

	@OneToMany(mappedBy = "apartment")
	private List<Bed> beds = new ArrayList<>();

	protected Apartment() {
	}

	public Long getId() {
		return id;
	}

	public Building getBuilding() {
		return building;
	}

	public String getCode() {
		return code;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public List<Bed> getBeds() {
		return beds;
	}
}
