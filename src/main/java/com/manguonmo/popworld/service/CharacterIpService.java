package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.CharacterIp;

import java.util.List;
import java.util.Optional;

public interface CharacterIpService {
    List<CharacterIp> getAllCharacterIps();
    Optional<CharacterIp> getCharacterIpById(Long id);
}
