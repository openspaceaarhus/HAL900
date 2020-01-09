#-*-perl-*-
package HAL::Page::LokalerResult;
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
use Digest::MD5 qw(md5_hex);

sub loadAllMembers() {

    my %members;
    my $paying = 0;
    my $mem = db->sql("select m.id, t.monthlyfee from member m join memberType t on (t.id=m.membertype_id)");
    while (my ($id, $fee) = $mem->fetchrow_array) {
	$members{$id} = {
	    fee=>$fee,
	};
	$paying++ if $fee > 1;
    }
    $mem->finish;

    return ($paying, \%members);
}

sub loadAllAnswers {
    my %answers;
    my $memberAnswers;
    my %memberWithAnAnswer;

    my $ans = db->sql("select member_id, space_option_id, points, comment from space_option_eval");
    
    while (my ($member_id, $space_option_id, $points, $comment) = $ans->fetchrow_array) {
	push @{$answers{$space_option_id}{$points}}, {
	    member=>$member_id,
	    comment=>$comment
	};
	$memberAnswers++ unless $memberWithAnAnswer{$member_id}++;    
    }
    $ans->finish;
    
    return $memberAnswers, \%answers;
}

sub loadOwnAnswers {
    my %own;    
    my $oa = db->sql("select space_option_id, points, comment from space_option_eval where member_id=?",
		     getSession->{member_id});
    while (my ($id, $points, $comment) = $oa->fetchrow_array) {
	$own{$id} = {
	    points=>$points,
	    comment=>$comment
	};
    }
    $oa->finish;

    return %own;
}

sub loadOptions {
    my ($own) = @_;
  
    my $missing=0;
    my %options;
    my $oq = db->sql("select id, title, uri, description from space_option order by title");
    my $error;
    while (my ($id, $title, $uri, $description) = $oq->fetchrow_array) {
	my $o = $own->{$id};
	$missing++ unless $o;
	$options{$id} = {
	    id=>$id,
	    title=>$title,
	    uri=>$uri,
	    desciption=>$description,
	    own=>$o,
	};
    }
    $oq->finish;

    return ($missing, \%options);
}

sub optionToColor {
    my ($option) = @_;

    my $start = substr(md5_hex($option->{title}), 0, 6);
    
    return qq'"#$start"';
}

sub datasetForOption {
    my ($option, $answers, $members) = @_;

    my $color = optionToColor($option);

    my @answers = ();    
    for my $points (sort {$a <=> $b} keys %$answers) {

	my $budget = 0;
	for my $bp (grep {$_ >= $points} keys %$answers) {
	    my $a = $answers->{$bp};
	    $budget += @$a * $points;
	}

	push @answers, "   { x:$points, y:$budget }";	
    }

    
    my $data = join ",\n", @answers;       

    return qq!
 {
  label: '$option->{title}',
  data: [
$data
  ],
  showLine: true,
  fill:false,
  borderColor: $color
 }!;
}

sub resultPage { 
  my ($r,$q,$p) = @_;

  my %own = loadOwnAnswers();
  my ($missing, $options) = loadOptions(\%own);
  return outputGoto("/hal/lokaler/eval?missing=$missing") if $missing;
  my ($membersWithAnAnswer, $answers) = loadAllAnswers();
  my ($paying, $members) = loadAllMembers();  

  my @options = map {$options->{$_}} sort {$options->{$a}{title} cmp $options->{$b}{title}} keys %$options;
  
  my $content = "";

  if ($p->{fresh}) {
      $content = "<p>Tak for dit svar, det betyder meget for OSAA at vide hvad du mener om fremtiden.</p>";
  }

  $content .= qq'<p>Totalt har vi fået svar fra $membersWithAnAnswer medlemmer, til sammenligning er der <a href="/hal/status/member-count?humans=1">$paying betalende medlemmer</a>.</p>';

  $content .= qq'<p>Du kan til en hver tid <a href="/hal/lokaler/eval">opdatere dine svar</a>.</p>';

  
  my %point;
  for my $o (keys %$answers) {
      for my $p (keys %{$answers->{$o}}) {
	  $point{$p}++;
      }
  }
  
  my $labels = join ',', sort { $a <=> $b } keys %point;

  my $datasets = join ',', map {
      datasetForOption($_, $answers->{$_->{id}}, $members);
  } @options;

  
  $content .= qq!<div class="chart-container" style="position: relative; height:40vh; width:80vw">
    <canvas id="chart"></canvas>
  </div>

<script>
var ctx = document.getElementById('chart').getContext('2d');
var myChart = new Chart(ctx, {
    type: 'scatter',
    labels: [$labels],
    data: {
        datasets: [ 
	   $datasets]
    },
    options: {
        scales: {
            yAxes: [{
                scaleLabel: {
                    display: true,
		    labelString: 'Budget',
                },
                ticks: {
                    beginAtZero: true
                }
            }],
            xAxes: [{
                scaleLabel: {
                    display: true,
		    labelString: 'Kontingent',
                }
            }]

        }
    }
});
</script>

<div style="min-height: 500px">
</div>
    !;


  return outputFrontPage("lokaler", "Lokaler", $content, undef, 'Chart.bundle.min.js');
}


BEGIN {
    addHandler(qr'^/hal/lokaler/result$', \&resultPage);
}

42;
