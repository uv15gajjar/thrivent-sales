package com.thrivent.automation;

import cucumber.api.CucumberOptions;
import cucumber.api.testng.AbstractTestNGCucumberTests;

@CucumberOptions(features = {"classpath:features"}, plugin = {"junit:reports/cucumber-results.xml"})

public class TestRunner extends AbstractTestNGCucumberTests {
}