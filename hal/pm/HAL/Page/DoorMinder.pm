#-*-perl-*-
package HAL::Page::DoorMinder;
use strict;
use warnings;
use utf8;

use Data::Dumper;

use HAL;
use HAL::Pages;
use HAL::Session;
use HAL::Util;

sub users {
    my ($r,$q,$p) = @_;

    my $content = "[\n";


    my $rs = db->sql('select rfid,pin from member m join rfid r on (r.owner_id=m.id) where dooraccess and not lost')
	or die "Fail!";
    my $sep = "\n";
    while (my ($rfid, $pin) = $rs->fetchrow_array) {
	$content .= $sep;
	$content .= qq'{"rfid":"$rfid", "pin":"$pin"}';
	$sep = ",\n";
    }
    $rs->finish;

    $content .= "]\n";
    
    return outputRaw("text/json", $content, "hal-users.yaml");
}


BEGIN {
    addHandler(qr'^/hal/admin/api/users$', \&users);
}

12;

