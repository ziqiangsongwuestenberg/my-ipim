package com.song.my_pim.repository;

import com.song.my_pim.entity.price.Price;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    Optional<Price> findByClientAndIdentifier(Integer client, String identifier);


}