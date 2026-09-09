package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CharacterIpServiceImpl implements CharacterIpService {

    private final CharacterIpRepository characterIpRepository;

    public CharacterIpServiceImpl(CharacterIpRepository characterIpRepository) {
        this.characterIpRepository = characterIpRepository;
    }

    @Override
    public List<CharacterIp> getAllCharacterIps() {
        return characterIpRepository.findAll();
    }

    @Override
    public Optional<CharacterIp> getCharacterIpById(Long id) {
        return characterIpRepository.findById(id);
    }
}
