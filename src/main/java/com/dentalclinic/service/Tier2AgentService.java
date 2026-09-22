package com.dentalclinic.service;

import com.dentalclinic.dto.Tier2AgentRequest;
import com.dentalclinic.dto.UpdateAgentStatusRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.AgentStatus;
import com.dentalclinic.model.AgentType;
import com.dentalclinic.model.Tier2Agent;
import com.dentalclinic.repository.Tier2AgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class Tier2AgentService {

    private final Tier2AgentRepository agentRepository;

    public Tier2AgentService(Tier2AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public Tier2Agent registerAgent(Tier2AgentRequest req) {
        String code = req.getAgentCode();
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Mã đại lý không được để trống!");
        }
        code = code.trim();
        if (agentRepository.existsByAgentCode(code)) {
            throw new BadRequestException("Mã đại lý đã tồn tại trong hệ thống: " + code);
        }

        Tier2Agent agent = new Tier2Agent();
        agent.setAgentCode(code);
        agent.setAgentName(req.getAgentName() != null ? req.getAgentName() : "Đại lý " + code);
        agent.setAgentType(req.getAgentType() != null ? req.getAgentType() : AgentType.SATELLITE_CLINIC);
        agent.setRepresentativeName(req.getRepresentativeName());
        agent.setPhone(req.getPhone());
        agent.setEmail(req.getEmail());
        agent.setAddress(req.getAddress());
        agent.setDistrict(req.getDistrict());
        agent.setCity(req.getCity());
        agent.setProvince(req.getProvince() != null ? req.getProvince() : req.getCity());
        agent.setContractDate(req.getContractDate() != null ? req.getContractDate() : LocalDate.now());
        agent.setCreditLimit(req.getCreditLimit() != null ? req.getCreditLimit() : 0.0);
        agent.setCommissionRate(req.getCommissionRate() != null ? req.getCommissionRate() : 0.0);
        agent.setCurrentBalance(0.0);
        agent.setOutstandingBalance(0.0);
        agent.setStatus(req.getStatus() != null ? req.getStatus() : AgentStatus.ACTIVE);
        agent.setActive(req.getActive() != null ? req.getActive() : true);

        return agentRepository.save(agent);
    }

    @Transactional(readOnly = true)
    public List<Tier2Agent> getAllAgents(AgentType agentType) {
        if (agentType != null) {
            return agentRepository.findByAgentType(agentType);
        }
        return agentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tier2Agent getAgentById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đại lý cấp 2 với ID: " + id));
    }

    public Tier2Agent updateAgentStatus(Long id, UpdateAgentStatusRequest req) {
        Tier2Agent agent = getAgentById(id);
        if (req.getCreditLimit() != null) {
            agent.setCreditLimit(req.getCreditLimit());
        }
        if (req.getActive() != null) {
            agent.setActive(req.getActive());
            agent.setStatus(req.getActive() ? AgentStatus.ACTIVE : AgentStatus.SUSPENDED);
        }
        if (req.getStatus() != null) {
            agent.setStatus(req.getStatus());
            agent.setActive(req.getStatus() == AgentStatus.ACTIVE);
        }
        return agentRepository.save(agent);
    }

    public Tier2Agent save(Tier2Agent agent) {
        return agentRepository.save(agent);
    }
}
