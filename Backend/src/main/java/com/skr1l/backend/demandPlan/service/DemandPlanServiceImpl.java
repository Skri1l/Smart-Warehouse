package com.skr1l.backend.demandPlan.service;

import com.skr1l.backend.demandPlan.DemandPlan;
import com.skr1l.backend.demandPlan.dto.DemandPlanRequestDto;
import com.skr1l.backend.demandPlan.dto.DemandPlanResponseDto;
import com.skr1l.backend.demandPlan.repository.DemandPlanRepository;
import com.skr1l.backend.exception.demandPlanExceptions.DemandPlanIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DemandPlanServiceImpl implements DemandPlanService {

    private static final AppLogger log = LoggerFactory.getLogger(DemandPlanServiceImpl.class);

    private final DemandPlanRepository demandPlanRepository;

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long createDemandPlan(DemandPlanRequestDto dto) {
        log.info("createDemandPlan");

        Objects.requireNonNull(dto, "dto is null");

        Product product = getProductEntityById(dto.productId());

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setProduct(product);
        demandPlan.setRequiredDate(dto.requiredDate());
        demandPlan.setRequiredQuantity(dto.requiredQuantity());

        demandPlanRepository.save(demandPlan);

        return demandPlan.getId();
    }

    @Override
    public DemandPlanResponseDto getDemandPlanById(Long id) {
        log.info("getDemandPlanById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getDemandPlanEntityById(id));
    }

    @Override
    public List<DemandPlanResponseDto> getAllDemandPlans() {
        log.info("getAllDemandPlans");

        return demandPlanRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<DemandPlanResponseDto> getDemandPlansByProductId(Long productId) {
        log.info("getDemandPlansByProductId");

        Objects.requireNonNull(productId, "productId is null");

        return demandPlanRepository.findByProductId(productId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<DemandPlanResponseDto> getDemandPlansByRequiredDate(LocalDate requiredDate) {
        log.info("getDemandPlansByRequiredDate");

        Objects.requireNonNull(requiredDate, "requiredDate is null");

        return demandPlanRepository.findByRequiredDate(requiredDate)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<DemandPlanResponseDto> getDemandPlansByDateRange(LocalDate from, LocalDate to) {
        log.info("getDemandPlansByDateRange");

        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");

        return demandPlanRepository.findByRequiredDateBetween(from, to)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<DemandPlanResponseDto> getFutureDemandPlans() {
        log.info("getFutureDemandPlans");

        return demandPlanRepository.findByRequiredDateAfter(LocalDate.now())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public DemandPlanResponseDto updateDemandPlan(Long id, DemandPlanRequestDto dto) {
        log.info("updateDemandPlan");

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        DemandPlan demandPlan = getDemandPlanEntityById(id);
        Product product = getProductEntityById(dto.productId());

        demandPlan.setProduct(product);
        demandPlan.setRequiredDate(dto.requiredDate());
        demandPlan.setRequiredQuantity(dto.requiredQuantity());

        return toResponseDto(demandPlan);
    }

    @Override
    @Transactional
    public void deleteDemandPlan(Long id) {
        log.info("deleteDemandPlan");

        Objects.requireNonNull(id, "id is null");

        DemandPlan demandPlan = getDemandPlanEntityById(id);
        demandPlanRepository.delete(demandPlan);
    }

    private DemandPlan getDemandPlanEntityById(Long id) {
        return demandPlanRepository.findById(id)
                .orElseThrow(() -> new DemandPlanIsNotFoundException(
                        "Demand plan with id " + id + " is not found"));
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductIsNotFoundException(
                        "Product with id " + id + " is not found"));
    }

    private DemandPlanResponseDto toResponseDto(DemandPlan demandPlan) {
        return new DemandPlanResponseDto(
                demandPlan.getId(),
                demandPlan.getProduct().getId(),
                demandPlan.getRequiredDate(),
                demandPlan.getRequiredQuantity());
    }
}