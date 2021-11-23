#-*-perl-*-
package HAL::Page::Bell;
use strict;
use warnings;
use utf8;
use Time::HiRes qw(sleep);

use Data::Dumper;

use HAL;
use HAL::Pages;
use HAL::Session;
use HAL::Util;

use YAML;

my $FILE = "/tmp/doorbell";

sub wait {
    my ($r,$q,$p) = @_;

    my $breakTime = time+10;
    my $mtime;
    while (time < $breakTime) {
	my $freshTime = -f $FILE ? (stat($FILE))[9] : 0;
	if ($mtime) {
	    if ($freshTime != $mtime) {
		return outputRaw("text/plain", "Ring");
	    }
	} else {
	    $mtime = $freshTime;
	}
	sleep 0.2;
    }
    
    return outputGoto("/hal/bell/wait?ts=".time);    
}

sub ring {
    my ($r,$q,$p) = @_;

    open B, ">$FILE" or die "Failed to write $FILE: $!";
    print B localtime."\n";
    close B;

    if ($p->{goto}) {
	return outputGoto($p->{goto});  
    } else {
	return outputRaw("text/plain", "Ok", "ok.txt");
    }
}

BEGIN {
    addHandler(qr'^/hal/bell/wait$', \&wait);
    addHandler(qr'^/hal/bell/ring$', \&ring);
}

12;
