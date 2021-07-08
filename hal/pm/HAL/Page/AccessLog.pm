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

sub accessPage {
    my ($r,$q,$p) = @_;
    
    my $limit = $p->{limit} || 200;
    die "Bad limit: $limit" unless $limit =~ /^\d+$/;
    
    my $rs = db->sql('select created_remote,d.name as device,t.name as type, t.id,event_number,wiegand_data,event_text
 from access_event e
 join access_device d on (d.id=e.device_id)
 join access_event_type t on (t.id=e.access_event_type)
 order by e.id desc limit ?', $limit)
	or die "Fail!";

    my $html = "<table><tr><th>Tid (UTC)</th><th>Enhed</th><th>Type</th><th>#</th><th>Data</th><th>Text</th></tr>\n";
    my $count = 0;
    while (my ($created, $device, $type, $type_id, $eventNumber, $data, $text) = $rs->fetchrow_array) {

	my @row = (
	    $created,
	    $device,
	    $type,
	    $eventNumber,
	    $data // '',
	    $text);
		     
	my $class = ($count++ & 1) ? 'class="odd"' : 'class="even"';
	$html .= qq'<tr class="$class">'.join('', map { "<td>$_</td>" } @row)."</tr>\n";

    }
    $html .= "</table>";
	
    $rs->finish;
   

    return outputAdminPage('accesslog', 'Access log', $html);
}
BEGIN {
    addHandler(qr'^/hal/admin/accesslog$', \&accessPage);
}

12;
