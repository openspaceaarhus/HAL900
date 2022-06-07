#-*-perl-*-
package HAL::Page::Status;
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
use HAL::Layout;

sub statusPage { 
  my ($r,$q,$p) = @_;

  my %dues;
  my $ct = db->sql("select target_account_id,comment,amount from accounttransaction where source_account_id >= 100001")
    or die "Urgh";
  my %sum;
  while (my ($id, $comment, $amount) = $ct->fetchrow_array) {
      my ($year, $month) = $comment =~ /^Kontingent (20\d+) 1\/(\d+)-/ or next;
      my $d = sprintf("%4d-%02d", $year, $month);
      $dues{$d}{$id}++;
      $sum{$d} += $amount;
  }
  $ct->finish;

  my $last = undef;
  my @table = [ qw'Month Count Sum Up Down Normalized' ];
  for my $d (sort keys %dues) {
      my @count = keys %{$dues{$d}};
      my @row = ($d, scalar(@count), $sum{$d});
      my ($up, $dn) = (0,0);
      if ($last) {
          for my $id (keys %{$dues{$d}}) {
              $up++ unless $dues{$last}{$id};
          }
          for my $id (keys %{$dues{$last}}) {
              $dn++ unless $dues{$d}{$id};
          }
      }
      push @row, $up, $dn, $sum{$d}/235;

      push @table, \@row;

      $last = $d;
  }

  if ($p->{csv}) {
    my $csv = "";
    for my $row (@table) {
	$csv .= join("\t", @$row)."\n";	
    }
    
    return {
      type=>'raw',
      mime=>'text/plain',
      content=>$csv
    };
    
  } elsif ($p->{wiki}) {

    my $wiki = '';
    my $rsep = '{|';
    my $csep = '!';
    for my $row (@table) {
	$wiki .= "$rsep\n";	
	for my $col (@$row) {
	    $wiki .= "$csep $col\n";;
	}
	$csep = '|';
	$rsep = '|-';	
    }
    $wiki .= "|}\n";
    
    return {
      type=>'raw',
      mime=>'text/plain',
      content=>$wiki
    };    
    
  } elsif ($p->{perl}) {
    return {
      type=>'raw',
      mime=>'text/plain',
      content=>Dumper \@table
    };    
    
  }

  shift @table; # Get rid of the header.
  
  my $labels = join ',', map {"'".$_->[0]."'"} @table;

  my $countCol;
  my $ccn;
  my $norm = !$p->{humans};
  if ($norm) {
	  $countCol = 5;
	  $ccn = qq'normaliseret til antal fuldt betalende medlemmer, så medlemsskaber med studie rabat tæller halvt. <a href="https://hal.osaa.dk/hal/status/member-count?humans=1">Skift</a>';
  } else {
	  $countCol = 1;
	  $ccn = qq'ikke normaliseret, så medlemsskaber med studie rabat tæller lige meget.  <a href="https://hal.osaa.dk/hal/status/member-count?humans=0">Skift til normaliseret</a>';
  }
  
  my $mid  = join ',', map {$_->[$countCol]} @table;    
  my $low  = join ',', map {$_->[$countCol]-$_->[3]} @table;    
  my $high = join ',', map {$_->[$countCol]+$_->[4]} @table;    
    
  my $content = qq!<div class="chart-container" style="position: relative; height:40vh; width:80vw">
    <canvas id="chart"></canvas>
    <ul>
<li>Dette er antallet af betalte kontingenter per måned, $ccn</li>
<li>High er det antal kontingenter der ville have været hvis ingen havde nedgraderet til gratis-medlem.</li>
<li>Low er det antal kontingenter der ville have været hvis ingen nye betalende medlemmer var kommet til.</li>
</ul>

</div>

<script>
var ctx = document.getElementById('chart').getContext('2d');
var myChart = new Chart(ctx, {
    type: 'line',
    data: {
        labels: [$labels],
        datasets: [{
            label: 'Members',
            data: [$mid],
            fill: false,
            borderColor: "#000000"
        },{
            label: 'High',
            data: [$high],
            fill: false,
            pointStyle: 'line',
            borderColor: "#00aa00",
            showLine: false
        },{
            label: 'Low',
            data: [$low],
            fill: false,
            borderColor: "#aa0000",
            pointStyle: 'line',
            showLine: false            
        }]
    },
    options: {
        scales: {
            yAxes: [{
                ticks: {
                    beginAtZero: true
                }
            }]
        }
    }
});
</script>

<div style="min-height: 500px">
</div>
!;

  return outputFrontPage("status", "Status", $content, undef, 'Chart.bundle.min.js');
}


BEGIN {
    addHandler(qr'^/hal/status/member-count?$', \&statusPage);
}

42;
