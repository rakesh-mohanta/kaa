/*
 * Copyright 2014-2016 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.server.common.dao.service;

import static org.kaaproject.kaa.server.common.dao.impl.DaoUtil.convertDtoList;
import static org.kaaproject.kaa.server.common.dao.impl.DaoUtil.getDto;
import static org.kaaproject.kaa.server.common.dao.service.Validator.isValidSqlId;
import static org.kaaproject.kaa.server.common.dao.service.Validator.isValidSqlObject;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kaaproject.kaa.common.dto.KaaAuthorityDto;
import org.kaaproject.kaa.common.dto.TenantAdminDto;
import org.kaaproject.kaa.common.dto.TenantDto;
import org.kaaproject.kaa.common.dto.UserDto;
import org.kaaproject.kaa.server.common.dao.ApplicationService;
import org.kaaproject.kaa.server.common.dao.UserService;
import org.kaaproject.kaa.server.common.dao.exception.IncorrectParameterException;
import org.kaaproject.kaa.server.common.dao.impl.TenantDao;
import org.kaaproject.kaa.server.common.dao.impl.UserDao;
import org.kaaproject.kaa.server.common.dao.model.sql.Tenant;
import org.kaaproject.kaa.server.common.dao.model.sql.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserDao<User> userDao;
    @Autowired
    private TenantDao<Tenant> tenantDao;
    @Autowired
    private ApplicationService applicationService;

    @Override
    public TenantDto saveTenant(TenantDto tenantDto) {
        LOG.trace("Try to save tenant object: {}", tenantDto);
        TenantDto tenant = null;
        if (isValidSqlObject(tenantDto)) {
            Tenant checkTenant = tenantDao.findByName(tenantDto.getName());
            if (checkTenant == null || checkTenant.getId().toString().equals(tenantDto.getId())) {
                tenant = getDto(tenantDao.save(new Tenant(tenantDto)));
            } else {
                throw new IncorrectParameterException("Can't save tenant with same name");
            }
        }
        return tenant;
    }

    @Override
    public void removeTenantById(String tenantId) {
        LOG.debug("Try to remove tenant by id {}", tenantId);
        if (isValidSqlId(tenantId)) {
            tenantDao.removeById(tenantId);
        }
    }

    @Override
    public TenantDto findTenantByName(String name) {
        TenantDto tenantDto = null;
        if (StringUtils.isNotBlank(name)) {
            tenantDto = getDto(tenantDao.findByName(name));
        }
        return tenantDto;
    }

    @Override
    public TenantDto findTenantById(String id) {
        TenantDto tenantDto = null;
        if (isValidSqlId(id)) {
            tenantDto = getDto(tenantDao.findById(id));
        }
        return tenantDto;
    }

    @Override
    public UserDto saveUser(UserDto userDto) {
        UserDto user = null;
        if (isValidSqlObject(userDto)) {
            user = getDto(userDao.save(new User(userDto)));
        }
        return user;
    }

    @Override
    public void removeUserById(String id) {
        if (isValidSqlId(id)) {
            userDao.removeById(id);
        }
    }

    @Override
    public UserDto findUserByExternalUid(String externalUid) {
        UserDto userDto = null;
        if (StringUtils.isNotBlank(externalUid)) {
            userDto = getDto(userDao.findByExternalUid(externalUid));
        }
        return userDto;
    }

    @Override
    public UserDto findUserById(String id) {
        UserDto userDto = null;
        if (isValidSqlId(id)) {
            userDto = getDto(userDao.findById(id));
        }
        return userDto;
    }

    @Override
    public List<TenantDto> findAllTenants() {
        return convertDtoList(tenantDao.find());
    }

    @Override
    public List<UserDto> findAllUsers() {
        return convertDtoList(userDao.find());
    }

    @Override
    public List<TenantAdminDto> findAllTenantAdmins() {
        LOG.debug("Try to find all admin tenants");
        List<TenantDto> tenants = findAllTenants();
        List<TenantAdminDto> tenantAdmins = new ArrayList<TenantAdminDto>(tenants.size());
        for (TenantDto tenant : tenants) {
            TenantAdminDto tenantAdmin = new TenantAdminDto();
            tenantAdmin.setId(tenant.getId());
            tenantAdmin.setName(tenant.getName());
            List<User> users = userDao.findByTenantIdAndAuthority(tenant.getId(), KaaAuthorityDto.TENANT_ADMIN.name());
            if (!users.isEmpty()) {
                tenantAdmin.setUserId(users.get(0).getId().toString());
                tenantAdmin.setUsername(users.get(0).getUsername());
                tenantAdmin.setExternalUid(users.get(0).getExternalUid());
            }
            tenantAdmins.add(tenantAdmin);
        }
        return tenantAdmins;
    }

    @Override
    public TenantAdminDto saveTenantAdmin(TenantAdminDto tenantAdminDto) {
        TenantAdminDto tenantAdmin = new TenantAdminDto();
        if (isValidSqlObject(tenantAdminDto)) {
            TenantDto tenant = new TenantDto();
            tenant.setId(tenantAdminDto.getId());
            tenant.setName(tenantAdminDto.getName());
            tenant = saveTenant(tenant);
            tenantAdmin.setId(tenant.getId());
            tenantAdmin.setName(tenant.getName());
        }
        if (StringUtils.isEmpty(tenantAdminDto.getUserId()) || isValidSqlId(tenantAdminDto.getUserId())) {
            UserDto user = new UserDto();
            user.setId(tenantAdminDto.getUserId());
            user.setUsername(tenantAdminDto.getUsername());
            user.setExternalUid(tenantAdminDto.getExternalUid());
            user.setTenantId(tenantAdmin.getId());
            user.setAuthority(KaaAuthorityDto.TENANT_ADMIN);
            user = saveUser(user);
            tenantAdmin.setUserId(user.getId());
            tenantAdmin.setUsername(user.getUsername());
            tenantAdmin.setExternalUid(user.getExternalUid());
            tenantAdmin.setId(user.getTenantId());
        }
        return tenantAdmin;
    }

    @Override
    public void removeTenantAdminById(String tenantId) {
        if (isValidSqlId(tenantId)) {
            LOG.debug("Remove by tenant id [{}]", tenantId);
            tenantDao.removeById(tenantId);
        }
    }

    @Override
    public TenantAdminDto findTenantAdminById(String id) {
        TenantAdminDto tenantAdminDto = null;
        if (isValidSqlId(id)) {
            TenantDto tenantDto = getDto(tenantDao.findById(id));
            if (tenantDto != null) {
                tenantAdminDto = new TenantAdminDto();
                tenantAdminDto.setId(tenantDto.getId());
                tenantAdminDto.setName(tenantDto.getName());
                List<User> users = userDao.findByTenantIdAndAuthority(id, KaaAuthorityDto.TENANT_ADMIN.name());
                if (!users.isEmpty()) {
                    tenantAdminDto.setUserId(users.get(0).getId().toString());
                    tenantAdminDto.setUsername(users.get(0).getUsername());
                    tenantAdminDto.setExternalUid(users.get(0).getExternalUid());
                }
            }
        }
        return tenantAdminDto;
    }

    @Override
    public List<UserDto> findAllTenantUsers(String tenantId) {
        if (isValidSqlId(tenantId)) {
            return convertDtoList(userDao.findByTenantIdAndAuthorities(tenantId,
                    KaaAuthorityDto.TENANT_DEVELOPER.name(),
                    KaaAuthorityDto.TENANT_USER.name()));
        } else {
            return null; //NOSONAR
        }
    }
}
