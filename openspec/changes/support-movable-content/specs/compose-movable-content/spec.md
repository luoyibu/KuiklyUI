## ADDED Requirements

### Requirement: movableContentOf nodes preserve state across container moves
The Compose DSL layer SHALL support `movableContentOf` such that when a composable subtree moves between parent containers, all `remember` state within that subtree MUST be preserved.

#### Scenario: Basic move between Column and Row (all platforms)
- **GIVEN** a `movableContentOf` block containing a composable with `remember` state
- **WHEN** the content moves from a Column parent to a Row parent via conditional composition
- **THEN** the composable's `remember` state (e.g., a counter) SHALL retain its value after the move

#### Scenario: Cross-container move with nested state (all platforms)
- **GIVEN** a `movableContentOf` block containing nested composables each with their own `remember` state
- **WHEN** the content is moved between two independent Box containers (left panel → right panel)
- **THEN** all nested `remember` states SHALL be preserved

### Requirement: KNode removeAt uses lightweight removal
When a KNode is removed via `removeAt`, the system SHALL NOT call `didRemoveFromParentView()` on the underlying view. The system SHALL only remove the view from the children list and clear `parentRef`.

#### Scenario: removeAt does not destroy view state (all platforms)
- **WHEN** `KNode.removeAt` is called on a node
- **THEN** the node's view SHALL NOT have its `ReactiveObserver` removed
- **AND** the node's `attr.viewDidRemove()` SHALL NOT be called
- **AND** the node's `event.onViewDidRemove()` SHALL NOT be called
- **AND** the node's `renderView` SHALL NOT be set to null

#### Scenario: removeAt removes from DOM and children (all platforms)
- **WHEN** `KNode.removeAt` is called on a node
- **THEN** `removeDomSubView` SHALL be called (flex node removal)
- **AND** the view SHALL be removed from the parent's `children` list
- **AND** the view's `parentRef` SHALL be set to 0

### Requirement: KNode detach clears nativeViewRef registration
When a KNode is detached (via LayoutNode.onChildRemoved → detach), it SHALL remove the view's nativeRef from the pager view map to maintain consistency.

#### Scenario: detach clears view map entry (all platforms)
- **GIVEN** a KNode that has been initialized (`isInitialized == true`)
- **WHEN** `KNode.detach()` is called
- **THEN** `getPager().removeNativeViewRef(view.nativeRef)` SHALL be called

### Requirement: KNode insertTopDown distinguishes first insert from reinsert
`insertTopDown` SHALL use the `isInitialized` flag to determine whether to perform full initialization or lightweight reinsertion.

#### Scenario: First insertion performs full init (all platforms)
- **GIVEN** a KNode with `isInitialized == false`
- **WHEN** `insertTopDown` is called
- **THEN** `addChild(view, init, index)` SHALL be called (triggering `willInit`/`init()`/`didInit`)
- **AND** `isInitialized` SHALL be set to `true`
- **AND** `insertDomSubView` SHALL be called

#### Scenario: Reinsert after move skips init (all platforms)
- **GIVEN** a KNode with `isInitialized == true` (previously removed and being re-inserted)
- **WHEN** `insertTopDown` is called
- **THEN** `reinsertChild(view, index)` SHALL be called (registers to new parent without re-init)
- **AND** `insertDomSubView` SHALL be called
- **AND** `willInit()`/`init()`/`didInit()` SHALL NOT be called

### Requirement: KNode onRelease performs full view cleanup
When a KNode receives `onRelease()` (node is permanently destroyed), it SHALL execute the full cleanup that was previously done in `didRemoveFromParentView`.

#### Scenario: onRelease cleans up all view resources (all platforms)
- **GIVEN** a KNode with `isInitialized == true`
- **WHEN** `onRelease()` is called
- **THEN** `ReactiveObserver.removeObserver(view)` SHALL be called
- **AND** `view.attr.viewDidRemove()` SHALL be called
- **AND** `view.event.onViewDidRemove()` SHALL be called
- **AND** `view.flexNode.layoutFrameDidChangedCallback` SHALL be set to null
- **AND** `view.renderView` SHALL be set to null

### Requirement: ViewContainer provides move-semantic methods
ViewContainer SHALL provide `removeChildForMove`, `reinsertChild`, and `removeChildrenForMoveAll` methods for the Compose integration layer to use during node moves.

#### Scenario: removeChildForMove performs lightweight removal (all platforms)
- **WHEN** `removeChildForMove(child)` is called
- **THEN** `child.willRemoveFromParentView()` SHALL be called
- **AND** the child SHALL be removed from the `children` list
- **AND** `child.parentRef` SHALL be set to 0
- **AND** `child.didRemoveFromParentView()` SHALL NOT be called

#### Scenario: reinsertChild registers to new parent (all platforms)
- **WHEN** `reinsertChild(child, index)` is called
- **THEN** `child.pagerId` SHALL be set to the new parent's pagerId
- **AND** `child.willMoveToParentComponent()` SHALL be called
- **AND** the child SHALL be added to `children` at `index`
- **AND** `child.parentRef` SHALL be set to the new parent's `nativeRef`
- **AND** `child.didMoveToParentView()` SHALL be called (re-registering nativeViewRef)

### Requirement: LayoutNode insertAt maintains original preconditions
`LayoutNode.insertAt` SHALL maintain its precondition checks that `instance._foldedParent == null` and `instance.owner == null`.

#### Scenario: insertAt rejects node with existing parent (all platforms)
- **GIVEN** a LayoutNode instance that still has a non-null `_foldedParent`
- **WHEN** `insertAt` is called with this instance
- **THEN** the system SHALL throw a precondition failure

### Requirement: Existing non-movableContent scenarios remain unaffected
The refactored lifecycle MUST NOT break existing LazyList scrolling, SubcomposeLayout reuse, or normal composable creation/destruction.

#### Scenario: LazyColumn scrolling with item recycling (all platforms)
- **GIVEN** a LazyColumn with 100+ items using SubcomposeLayout
- **WHEN** the user scrolls through the list, causing items to be composed and disposed
- **THEN** items SHALL render correctly without state leaks or visual glitches
- **AND** the pager view map SHALL NOT accumulate stale entries

#### Scenario: SubcomposeLayout slot disposal (all platforms)
- **GIVEN** a SubcomposeLayout that dynamically creates and removes slots
- **WHEN** a slot is disposed (composition.dispose() + root.removeAt)
- **THEN** the VirtualNodeView's nativeViewRef SHALL be cleaned from the pager view map via detach()
- **AND** no memory leaks SHALL occur from attr/event (VirtualNodeView uses empty implementations)
