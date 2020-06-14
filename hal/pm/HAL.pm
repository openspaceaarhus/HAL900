#-*-perl-*-
package HAL;
require Exporter;
@ISA=qw(Exporter);
@EXPORT = qw(testMode HALRoot emailSalt getDBUrl configureHAL getDBUser getDBPassword);

use strict;
use warnings;

my $config;
sub testMode() {
    return $config->{test};
}

sub HALRoot() {
    return $config->{root};
}
sub emailSalt() {
    return $config->{salt};
}

sub getDBUrl() {
    return $config->{db};
}

sub getDBUser {
    return $config->{dbuser} || 'hal';
}

sub getDBPassword {
    return $config->{dbPassword} ||'hal900';
} 

# Configure the system from docker conventions
sub dockerConfig() {

    my %e;
    open E, "/home/hal/hal/config/docker.env" or die "Missing /home/hal/hal/config/docker.env: $!";
    while (my $line = <E>) {
	chomp $line;
	my ($k, $v) = split /=/, $line;
	$e{$k} = $v;
    }
    close E;
    
    my $host = $e{PGHOST} or die "Missing PGHOST";
    my $port = $e{PGPORT} or die "Missing PGPORT";
    my $db = $e{PGDATABASE} or die "Missing PGDATABASE";
    my $user = $e{PGUSER} or die "Missing PGUSER";
    my $pass = $e{PGPASSWORD} or die "Missing password PGPASSWORD";

    return {
	root=>"/home/hal/hal",
	test=>1,
	dbUser=>$user,
	dbPassword=>$pass,
	db=>"dbi:Pg:host=$host;port=$port;dbname=$db",
    };
}

sub configureHAL {
    my $host = shift;

    if (-f "/.dockerenv") {
	$host ||= 'docker';
    } else {
	$host ||= `hostname`;
	chomp $host;
    }

    die "HOST environment variable not defined, cannot self-configure." unless $host;

    my %CONFIG = (
	panther=>{
	    root=>"/home/ff/projects/osaa/HAL900/hal",
	    test=>1,
	    db=>'dbi:Pg:dbname=hal;port=5432',
	},
	bee=>{
	    root=>"/home/ff/projects/osaa/HAL900/hal",
	    test=>1,
	    db=>'dbi:Pg:dbname=hal;host=localhost;port=5433',
	},
	hal=>{
	    root=>"/home/hal/hal",
	    test=>0,
	    db=>'dbi:Pg:dbname=hal;port=5432'	    
	},
	lisbeth=>{
	    root=>"/home/jacob/Desktop/HACK/hal",
	    test=>1,
	    db=>'dbi:Pg:dbname=hal;port=5432',
	},
	blade=>{
	    root=>"/home/ff/projects/HAL900/hal",
	    test=>1,
	    db=>'dbi:Pg:dbname=hal;user=ff;port=5432',
	},
	
	docker=>dockerConfig()

    );
    
    $config = $CONFIG{$host} || die "HOST=$host is not found in \%CONFIG, cannot self-configure, fix pm/HAL.pm";

    my $sf = "$config->{root}/.hal/web-salt.txt";
    unless (-f $sf) {
	mkdir "$config->{root}/.hal";

	my $k = '';
	my $sep = '';
	open R, "</dev/urandom" or die "Fail: $!";
	for my $i (0..31) {
	    my $data;
	    read(R, $data, 1) or die "Couldn't read from /dev/urandom";
	    my $byte = unpack('C',$data);
	    $k .= sprintf("%0x", $byte);
	}
	close R;

	open K, ">$sf" or die "Failed to wite $sf: $!";
	print K $k;
	close K;
    }

    open K, "<$sf" or die "Failed to read $sf: $!";
    $config->{salt} = join '', <K>;
    close K;
}

BEGIN {
    configureHAL();
}

42;
