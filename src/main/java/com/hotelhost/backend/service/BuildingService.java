package com.hotelhost.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelhost.backend.dto.BuildingResponse;
import com.hotelhost.backend.repository.BuildingRepository;

@Service
public class BuildingService {

	private final BuildingRepository buildingRepository;

	public BuildingService(BuildingRepository buildingRepository) {
		this.buildingRepository = buildingRepository;
	}

	@Transactional(readOnly = true)
	public List<BuildingResponse> findAll() {
		return buildingRepository.findAllWithApartmentCount();
	}
}
