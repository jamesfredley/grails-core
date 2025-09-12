package com.example.community

import grails.gorm.transactions.Transactional
import grails.plugin.scaffolding.annotation.Scaffold

import grails.plugin.scaffolding.GormService

@Scaffold(GormService<User>)
@Transactional
class CommunityUserService {}
