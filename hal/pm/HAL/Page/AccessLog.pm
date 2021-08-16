#-*-perl-*-
package HAL::Page::AccessLog;
use strict;
use warnings;
use utf8;

use Data::Dumper;
use HTML::Entities;
use Digest::SHA qw(sha1_hex);
use POSIX;
use Text::vCard;
use Text::vCard::Addressbook;
#use GD::Barcode::QRcode;

use HAL;
use HAL::Pages;
use HAL::Session;
use HAL::Util;
use HAL::Email;
use HAL::TypeAhead;

my %ownerToLink;
sub rfidOwnerLink {
    my ($rfid) = @_;

    return _rfidOwnerLink($rfid) || _rfidOwnerLink(($rfid >> 1) & 0xffffffff);
}

sub _rfidOwnerLink {
    my ($rfid) = @_;

    return undef unless defined $rfid;
    
    return $ownerToLink{$rfid} if exists $ownerToLink{$rfid};
    
    my $mr = db->sql('select m.id, realname, username, r.name, r.id, r.rfid from member m join rfid r on (r.owner_id=m.id) where r.rfid=?', $rfid);
    my ($mid, $realname,$username,$rfidName,$rid, $fdid) = $mr->fetchrow_array;
    $mr->finish;

    return $ownerToLink{$rfid} = undef unless defined $mid;

    $rfidName ||= $rfid;
    return $ownerToLink{$rfid} = qq'RFID <a href="/hal/admin/rfid/$rid">$rfidName</a> belonging to <a href="/hal/admin/members/$mid">$realname aka. $username</a>';  
}


sub accessPage {
    my ($r,$q,$p) = @_;
    %ownerToLink = ();
    
    my $limit = $p->{limit} || 1000;
    die "Bad limit: $limit" unless $limit =~ /^\d+$/;

    my $rb = db->sql("select device_id, index, name, set_event, clear_event from gpio_bit");
    my %gb;
    while (my ($did, $index, $name, $setEvent, $clearEvent) = $rb->fetchrow_array) {
	my $bitId = "$did.$index";
	
	$gb{$bitId} = {
	    name=>$name,
	    setEvent=>$setEvent,
	    clearEvent=>$clearEvent,
	};
    }
    
    $rb->finish;
    
    my $rs = db->sql('select * from (select created_remote,d.name as device,t.name as type, t.id,event_number,wiegand_data,event_text, e.id as eid, e.device_id
 from access_event e
 join access_device d on (d.id=e.device_id)
 join access_event_type t on (t.id=e.access_event_type)
 order by e.id desc limit ?) as subselect order by eid', $limit);

    my @rows;
    my $gpioStateByDevice = 0;
    while (my ($created, $device, $type, $type_id, $eventNumber, $data, $text, $eventId, $deviceId) = $rs->fetchrow_array) {

	if ($type_id == 255) {
	    my $ul = rfidOwnerLink($data);
	    if ($ul) {
		$text = qq'PIN timeout for $ul';
	    }
	    
	} elsif ($type_id == 254) {
	    my $ul = rfidOwnerLink($data);
	    $text = qq'Unlocked for $ul';
	    
	} elsif ($type_id == 1 && $text =~ /^RFID/) {	    
	    my $ul = rfidOwnerLink($data);
	    if (defined $ul) {
		$text = qq'Scanned $ul';
	    }
	} elsif ($type_id == 3) {

	    my @full;
	    my @flips;
	    for my $bit (0..7) {
		my $mask = 1<<$bit;
		my $old = $gpioStateByDevice & $mask;
		my $new = $data & $mask;
		my $bitId = "$deviceId.$bit";
		my $gb = $gb{$bitId};
		if ($gb) {
		    push @full, "$gb->{name}=".($new ? '1' : '0');

		    if ($old != $new) {
			my $event = $new ? 'setEvent' : 'clearEvent';
			if (my $et = $gb->{$event}) {
			    push @flips, $et;
			}
		    }		
		} else {
		    push @full, "$bitId=".($new ? '1' : '0');			
		}
	    }

	    my $full = join ', ', @full;
	    $text = qq'<span title="$full">GPIO: '.join(", ", @flips)."</span>";
	    
	    $gpioStateByDevice = $data;
	}	
	
	my @row = (
	    $created,
	    $device,
	    $type,
	    $eventNumber,
	    $data // '',
	    $text);

	unshift @rows, \@row;
    }
    

    my $html = "<table><tr><th>Tid (UTC)</th><th>Enhed</th><th>Type</th><th>#</th><th>Data</th><th>Text</th></tr>\n";
		     
    my $count = 0;
    for my $row (@rows) {
	my $class = ($count++ & 1) ? 'class="odd"' : 'class="even"';
	$html .= qq'<tr class="$class">'.join('', map { "<td>$_</td>" } @$row)."</tr>\n";
    }    
    $html .= "</table>";
	
    $rs->finish;
   

    return outputAdminPage('accesslog', 'Access log', $html);
}
BEGIN {
    addHandler(qr'^/hal/admin/accesslog$', \&accessPage);
}

12;
