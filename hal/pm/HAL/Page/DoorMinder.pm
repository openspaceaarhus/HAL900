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

use YAML;

sub state {
    my ($r,$q,$p) = @_;

    my $state = {};

    my $rs = db->sql('select rfid,pin from member m join rfid r on (r.owner_id=m.id) where dooraccess and not lost')
	or die "Fail!";
    while (my ($rfid, $pin) = $rs->fetchrow_array) {
	$state->{rfidToPin}{$rfid} = $pin;
    }
    $rs->finish;

    
    my $dr = db->sql('select id,name,aesKey from access_device where id <> 0') or die "Fail!";
    my $sep = "\n";
    while (my ($id, $name, $aesKey) = $dr->fetchrow_array) {
	$state->{devices}{$id} = {
	    id=>$id,
	    name=>$name,
	    aesKey=>$aesKey,	    
	};
    }
    $dr->finish;
    

    return outputRaw("text/yaml", Dump($state), "hal-state.yaml");
}


sub getDevices {

    my %res;
    my $rs = db->sql('select id, name, aesKey from access_device') or die "Fail!";
    my $sep = "\n";
    while (my ($id,$name,$key) = $rs->fetchrow_array) {
	$res{$id} = {
	    name=>$name,
	    key=>$key
	};
    }
    $rs->finish;


    return \%res;
}

sub getTypes {

    my %res;
    my $rs = db->sql('select id, name from access_event_type') or die "Fail!";
    my $sep = "\n";
    while (my ($id,$name) = $rs->fetchrow_array) {
	$res{$id} = {
	    name=>$name,
	}
    }
    $rs->finish;


    return \%res;
}

sub events {
    my ($r,$q,$p) = @_;

    my $devices = getDevices();
    my $types = getTypes();

    my $data = $p->{POSTDATA} or die "Missing post data";

    for my $line (split /\n/, $data) {
#1624955447305	1	1	24	5	Wiegand 4 bits: 0x5 (04 05 00 00 00 00 00 00)	
	my ($ts, $deviceId, $typeId, $number, $data, $text) = split /\t/, $line;
	
	if (!$text) {
	    $text = $data;
	    $data = undef;	    
	}

	if (!$devices->{$deviceId}) {
	    db->sql("insert into access_device (id,name,aesKey) values (?,'Unknown','')", $deviceId);
	    $devices = getDevices();
	    print STDERR "Inserted new device: $deviceId\n";
	}
	if (!$types->{$typeId}) {
	    db->sql("insert into access_event_type (id,name) values (?,'Unknown '||?)", $typeId, $typeId);
	    $types = getTypes();
	    print STDERR "Inserted new type: $typeId\n";
	}

	
	
	db->sql("insert into access_event (created_remote, device_id, access_event_type, event_number, wiegand_data, event_text) ".
		" values (timestamp 'epoch'+?*interval '1 ms',     ?,         ?,                    ?,            ?,            ?) ".
		"on conflict do nothing",
		$ts, $deviceId, $typeId, $number, $data, $text) or die "Failed to insert $line";
    }
    
    
    return outputRaw("text/plain", "Ok", "ok.txt");    
}

sub createDevices {
    my ($r,$q,$p) = @_;

    my $oldDevices = getDevices();
    
    my $data = $p->{POSTDATA} or die "Missing post data";
    my $devices = Load($data);
    for my $d (@$devices) {
	if (my $od = $oldDevices->{$d->{id}}) {
	    db->sql("update access_device set aesKey=? where id=?",
		    $d->{aesKey}, $d->{id}
		) or die;	    
	} else {
	    db->sql("insert into access_device (id,name,aesKey) values (?,?,?)",
		    $d->{id}, $d->{name}, $d->{aesKey}
		) or die;	    
	}
	
    }
    
    return outputRaw("text/plain", "Ok", "ok.txt");    
}


BEGIN {
    ensureAPI(qr'^/hal/api/');
    addHandler(qr'^/hal/api/events$', \&events);
    addHandler(qr'^/hal/api/state$', \&state);
    addHandler(qr'^/hal/api/createDevices$', \&createDevices);
}

12;
