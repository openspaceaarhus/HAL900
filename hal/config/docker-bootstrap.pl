#!/usr/bin/perl
use strict;
use warnings;
use lib "/home/hal/hal/pm";
use HAL;
use HAL::UI;

configureHAL("docker");
HAL::UI::bootStrap();

1;
