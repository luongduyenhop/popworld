package com.manguonmo.popworld.service;

import com.manguonmo.popworld.service.impl.CharacterIpServiceImpl;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterIpServiceTest {

    @Mock
    private CharacterIpRepository characterIpRepository;

    @InjectMocks
    private CharacterIpServiceImpl characterIpService;

    @Test
    @DisplayName("getAllCharacterIps trả về toàn bộ danh sách nhân vật IP")
    void getAllCharacterIps_shouldReturnList() {
        CharacterIp ip1 = CharacterIp.builder().id(1L).name("Molly").build();
        CharacterIp ip2 = CharacterIp.builder().id(2L).name("Skullpanda").build();

        when(characterIpRepository.findAll()).thenReturn(List.of(ip1, ip2));

        List<CharacterIp> result = characterIpService.getAllCharacterIps();

        assertEquals(2, result.size());
        assertEquals("Molly", result.get(0).getName());
        verify(characterIpRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getCharacterIpById trả về Optional CharacterIp theo id")
    void getCharacterIpById_shouldReturnOptional() {
        CharacterIp ip = CharacterIp.builder().id(1L).name("Hirono").build();

        when(characterIpRepository.findById(1L)).thenReturn(Optional.of(ip));

        Optional<CharacterIp> result = characterIpService.getCharacterIpById(1L);

        assertTrue(result.isPresent());
        assertEquals("Hirono", result.get().getName());

        when(characterIpRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<CharacterIp> notFound = characterIpService.getCharacterIpById(99L);
        assertTrue(notFound.isEmpty());
    }
}
