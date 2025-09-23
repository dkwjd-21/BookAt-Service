package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.Address;

@Mapper
public interface AddressMapper {
    
    List<Address> findByUserId(String userId);
    Address findDefaultByUserId(String userId);
    int insert(Address address);
    int update(Address address);
    int delete(int addrId);
}
