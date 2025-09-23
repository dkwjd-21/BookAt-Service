package com.bookat.service;

import java.util.List;

import com.bookat.entity.Address;

public interface AddressService {
    List<Address> getAddressesByUserId(String userId);
    Address getDefaultAddressByUserId(String userId);
    void saveAddress(Address address);
    void updateAddress(Address address);
    void deleteAddress(int addrId);
}
