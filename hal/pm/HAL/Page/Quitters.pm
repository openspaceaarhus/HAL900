#-*-perl-*-
package HAL::Page::AdminQuitters;
use strict;
use warnings;
use utf8;

use Data::Dumper;
use HTML::Entities;
use Digest::SHA qw(sha1_hex);

use HAL;
use HAL::Pages;
use HAL::Session;
use HAL::Util;
use HAL::Email;

sub quitters {
    my ($r,$q,$p) = @_;

    my $days = "365 days";
    if ($p->{days} && $p->{days} =~ /^(\d+)$/) {
	$days = "$1 days";
    }

    my %deletedRfid;
    my $res = db->sql("select t.created, owner_id, kind
                       from  rfid r 
                       join doortransaction t on (t.rfid_id=r.id) 
                       where age(t.created) < '$days'                      
                       order by t.id") 
	or die "Failed to get list of deleted rfids";

    while (my ($created, $memberId, $kind) = $res->fetchrow_array) {
	if ($kind eq 'd') {
	    $deletedRfid{$memberId} = $created;
	} else {
	    delete $deletedRfid{$memberId};
	}
    }
    $res->finish;

    # Users who have downgraded, but still in possession of a door bit:
    my $hot = db->sql("select updated, id from member where dooraccess and membertype_id=2") 
	or die "Failed to get list of deleted rfids";

    while (my ($created, $memberId) = $hot->fetchrow_array) {
	$deletedRfid{$memberId} = $created;
    }
    $hot->finish;
    
    my @quitters;
    for my $id (keys %deletedRfid) {
	my $mr = db->sql("select m.created, username, realname, email, membertype_id, m.dooraccess, membertype 
			 from member m
			 join membertype t on (m.membertype_id=t.id)
                         where m.id=?", $id);
	my $dude = $mr->fetchrow_hashref;
	$mr->finish;

	if (!$dude->{dooraccess} || $dude->{membertype_id} == 2) {
	    $dude->{quit} = $deletedRfid{$id};
	    $dude->{id} = $id;
	    push @quitters, $dude;
	}
    }

    @quitters = sort { $a->{quit} cmp $b->{quit} }  @quitters;

    my $cnt = scalar(@quitters);
    my $html = "
             <p>In the last $days days there have been $cnt members who have downgraded:</p>
             <table><tr><th>Quit Date</th><th>Join Date</th><th>User</th>	    <th>Navn</th>
	    <th>Email</th>
	    <th>Type</th>
	    <th>Dooraccess</th></tr>\n";

    my $count = 0;
    for my $q (@quitters) {
	my $class = ($count++ & 1) ? 'class="odd"' : 'class="even"';

	my $join = $q->{created};
	$join =~ s/\.\d+$//;
	my $quit = $q->{quit};
	$quit =~ s/\.\d+$//;
	
	$html .= qq'<tr $class>
	    <td>$quit</td>
	    <td>$join</td>
	    <td><a href="/hal/admin/members/$q->{id}">$q->{username}</a></td>
	    <td>$q->{realname}</td>
	    <td>$q->{email}</td>
	    <td>$q->{membertype}</td>
	    <td>$q->{dooraccess}</td>
            </tr>';
    }

    $html .= "</table>";
    
    return HAL::Page::Admin::outputAdminPage('quitters', 'Quitters', $html);
}


BEGIN {
    addHandler(qr'^/hal/admin/quitters$', \&quitters);
}

12;
