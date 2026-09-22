package com.dentalclinic.service;

import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.LoyaltyAccount;
import com.dentalclinic.model.LoyaltyTier;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.LoyaltyAccountRepository;
import com.dentalclinic.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyRepository;
    private final UserRepository userRepository;

    public LoyaltyService(LoyaltyAccountRepository loyaltyRepository, UserRepository userRepository) {
        this.loyaltyRepository = loyaltyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LoyaltyAccount getAccountByPhone(String phone) {
        return loyaltyRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản thành viên không tồn tại cho số điện thoại: " + phone));
    }

    public LoyaltyAccount getOrCreateAccount(User patient, String phone) {
        Optional<LoyaltyAccount> existing = loyaltyRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (patient == null) {
            patient = userRepository.findByPhone(phone).orElse(null);
        }

        LoyaltyAccount account = new LoyaltyAccount(patient, phone, 0, LoyaltyTier.SILVER, 0);
        return loyaltyRepository.save(account);
    }

    public LoyaltyAccount addPoints(String phone, int points, String reason) {
        if (points <= 0) return getOrCreateAccount(null, phone);

        LoyaltyAccount account = getOrCreateAccount(null, phone);
        account.setPointsBalance(account.getPointsBalance() + points);
        account.setTotalPointsEarned(account.getTotalPointsEarned() + points);
        updateTier(account);
        return loyaltyRepository.save(account);
    }

    public LoyaltyAccount redeemPoints(String phone, int points) {
        LoyaltyAccount account = getAccountByPhone(phone);
        if (account.getPointsBalance() < points) {
            throw new BadRequestException("Số dư điểm (" + account.getPointsBalance() + ") không đủ để đổi (" + points + " điểm)!");
        }
        account.setPointsBalance(account.getPointsBalance() - points);
        return loyaltyRepository.save(account);
    }

    private void updateTier(LoyaltyAccount account) {
        int total = account.getTotalPointsEarned();
        if (total >= 5000) {
            account.setMembershipTier(LoyaltyTier.DIAMOND);
        } else if (total >= 2000) {
            account.setMembershipTier(LoyaltyTier.PLATINUM);
        } else if (total >= 500) {
            account.setMembershipTier(LoyaltyTier.GOLD);
        } else {
            account.setMembershipTier(LoyaltyTier.SILVER);
        }
    }
}
