package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookat.entity.Address;
import com.bookat.mapper.AddressMapper;
import com.bookat.service.AddressService;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getAddressesByUserId(String userId) {
        return addressMapper.findByUserId(userId);
    }

    @Override
    public Address getDefaultAddressByUserId(String userId) {
        return addressMapper.findDefaultByUserId(userId);
    }

    @Override
    public void saveAddress(Address address) {
        addressMapper.insert(address);
    }

    @Override
    public void updateAddress(Address address) {
        addressMapper.update(address);
    }

    @Override
    public void deleteAddress(int addrId) {
        addressMapper.delete(addrId);
    }
}
