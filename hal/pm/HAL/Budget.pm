#-*-perl-*- $
package HAL::Budget;
require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw(currentBudget);

use strict;
use warnings;
use HAL::Pages;

sub currentBudget {
    my $mcRes = (db->sql('select count(*)
      from member m
      join membertype t on (t.id=m.membertype_id)
      where monthlyfee > 0'));
    my ($memberCount) = $mcRes->fetchrow_array;
    $mcRes->finish;
    
    my $bRes = (db->sql('select amount from budget where id = (select max(b2.id) from budget b2)'));
    my ($currentBudget) = $bRes->fetchrow_array;
    $bRes->finish;
    
    my $breakevenFee = int($currentBudget/$memberCount)+1; 

  
    return {
      memberCount => $memberCount,
      currentBudget => $currentBudget,
      breakevenFee => $breakevenFee,
    };
}

1;
